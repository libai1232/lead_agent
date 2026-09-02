package com.example.leadagent.service;

import com.example.leadagent.dto.ChatRequest;
import com.example.leadagent.dto.ChatResponse;
import com.example.leadagent.exception.LlmDecisionException;
import com.example.leadagent.model.Action;
import com.example.leadagent.model.ConversationMessage;
import com.example.leadagent.model.ConversationRole;
import com.example.leadagent.model.CustomerSession;
import com.example.leadagent.model.LlmDecision;
import com.example.leadagent.model.SessionStatus;
import com.example.leadagent.service.llm.LlmService;
import com.example.leadagent.service.qualification.LeadQualificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 编排单条客户消息的 Agent 处理流程。
 *
 * <p>处理顺序为：会话静默检查、LLM 分析、确定性状态机裁决、生成 API 响应。</p>
 */
@Service
public class AgentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgentService.class);

    private final LlmService llmService;
    private final SessionService sessionService;
    private final SessionStateMachine sessionStateMachine;
    private final ActionExecutor actionExecutor;
    private final LeadQualificationService leadQualificationService;
    private final HumanHandoffSummaryService humanHandoffSummaryService;

    public AgentService(
            LlmService llmService,
            SessionService sessionService,
            SessionStateMachine sessionStateMachine,
            ActionExecutor actionExecutor,
            LeadQualificationService leadQualificationService,
            HumanHandoffSummaryService humanHandoffSummaryService
    ) {
        this.llmService = llmService;
        this.sessionService = sessionService;
        this.sessionStateMachine = sessionStateMachine;
        this.actionExecutor = actionExecutor;
        this.leadQualificationService = leadQualificationService;
        this.humanHandoffSummaryService = humanHandoffSummaryService;
    }

    /**
     * 处理一条客户消息。
     *
     * @param request 客户消息请求
     * @return 可发送的回复，或明确的静默结果
     */
    public ChatResponse handleMessage(ChatRequest request) {
        CustomerSession session = sessionService.getOrCreate(request.customerId());

        // 同一客户的整轮处理串行化，避免历史、转人工状态和最终发送在并发请求间交错。
        synchronized (session) {
            return handleActiveSession(request, session);
        }
    }

    /**
     * 在客户级锁内完成一整轮处理；不同 CustomerSession 之间仍可并行。
     */
    private ChatResponse handleActiveSession(ChatRequest request, CustomerSession session) {

        // 静默状态在调用 LLM 之前短路，保证转人工或关闭后不再自动处理。
        if (session.getStatus() != SessionStatus.ACTIVE) {
            return withDiagnostics(new ChatResponse(null, true), null, null, session);
        }

        LlmDecision decision;
        var history = session.getConversationHistorySnapshot();
        try {
            decision = llmService.analyze(request.message(), history);
        } catch (LlmDecisionException exception) {
            String causeType = exception.getCause() == null
                    ? "none"
                    : exception.getCause().getClass().getSimpleName();
            // 只记录类型信息，不记录客户输入、模型原文、customerId、配置或凭证。
            LOGGER.warn(
                    "LLM decision failed; returning silent response. exceptionType={}, causeType={}",
                    exception.getClass().getSimpleName(),
                    causeType
            );
            // 结构化输出失败时在状态机和动作执行器之前终止，且不暴露模型原始输出。
            return withDiagnostics(new ChatResponse(null, true), null, null, session);
        }

        try {
            // 初筛提取与动作裁决解耦；提取结果只有业务字段，不能控制 Session 或 Action。
            session.mergeLeadQualification(
                    leadQualificationService.extract(request.message(), history)
            );
        } catch (RuntimeException exception) {
            // 初筛是辅助能力，失败时只跳过本轮资料更新，不记录客户内容或模型原文。
            LOGGER.warn(
                    "Lead qualification failed; continuing without profile update. exceptionType={}",
                    exception.getClass().getSimpleName()
            );
        }

        // 只有决策成功的 ACTIVE 消息才进入有限历史，解析失败不会产生隐式状态修改。
        session.addConversationMessage(new ConversationMessage(
                ConversationRole.CUSTOMER,
                request.message()
        ));

        Action effectiveAction = sessionStateMachine.applyDecision(session, decision);

        if (effectiveAction == Action.ESCALATE_TO_HUMAN) {
            // 摘要在转人工动作前生成，但只读取已确认字段，不获得任何执行权限。
            session.setHandoffSummary(humanHandoffSummaryService.create(session, decision));
        }

        // AgentService 只负责编排，所有动作语义统一交给有限动作执行器。
        ChatResponse actionResponse = actionExecutor.execute(
                effectiveAction,
                session,
                decision.replyDraft()
        );

        if (!actionResponse.silent() && actionResponse.message() != null) {
            // 只保存真正发给客户的安全回复；被 Guard 替换时保存替换后的实际文本。
            session.addConversationMessage(new ConversationMessage(
                    ConversationRole.AGENT,
                    actionResponse.message()
            ));
        }
        return withDiagnostics(actionResponse, decision, effectiveAction, session);
    }

    /**
     * 为 Demo 响应附加只读诊断信息，便于观察状态机和限流结果。
     *
     * @param response 动作执行器产生的基础响应
     * @param decision LLM 决策；LLM 未调用或失败时为 {@code null}
     * @param action Java 裁决后的最终动作；尚未裁决时为 {@code null}
     * @param session 当前客户 Session
     * @return 包含完整 Demo 状态信息的响应
     */
    private ChatResponse withDiagnostics(
            ChatResponse response,
            LlmDecision decision,
            Action action,
            CustomerSession session
    ) {
        return new ChatResponse(
                response.message(),
                response.silent(),
                decision == null ? null : decision.intent(),
                decision == null ? null : decision.dissatisfied(),
                action,
                session.getStatus(),
                session.getAbnormalCount(),
                session.getLastSentAt(),
                session.getLeadProfile(),
                session.getStatus() == SessionStatus.HUMAN_ESCALATED
                        ? session.getHandoffSummary()
                        : null,
                session.getConversationHistorySnapshot().size()
        );
    }
}
