package com.example.leadagent.model;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * 单个客户的内存会话状态。
 *
 * <p>当前对象只保存在进程内，应用重启后数据会丢失。</p>
 */
public class CustomerSession {

    /** 最多保留最近十条已成功处理的客户/Agent 消息，防止上下文无限增长。 */
    private static final int MAX_HISTORY_MESSAGES = 10;

    /** 客户唯一标识，创建后不可变。 */
    private final String customerId;
    /** 当前会话状态；使用 volatile 保证跨线程读取最新值。 */
    private volatile SessionStatus status;
    /** 连续异常消息次数，OFF_TOPIC 与不满情绪共用该计数器。 */
    private volatile int abnormalCount;
    /** 最近一次实际发送消息的时间；预留给后续限流功能。 */
    private volatile Instant lastSentAt;
    /** 最近的有限对话历史；所有访问均在当前 Session 锁内完成。 */
    private final ArrayDeque<ConversationMessage> conversationHistory;
    /** 已累计的客户初筛字段，只能由服务端提取流程更新。 */
    private volatile LeadProfile leadProfile;
    /** 转人工后供人工查看的服务端摘要；未转人工时为 {@code null}。 */
    private volatile String handoffSummary;

    /**
     * 创建具有默认状态的客户 Session。
     *
     * @param customerId 客户唯一标识
     */
    public CustomerSession(String customerId) {
        this.customerId = customerId;
        this.status = SessionStatus.ACTIVE;
        this.abnormalCount = 0;
        this.lastSentAt = null;
        this.conversationHistory = new ArrayDeque<>();
        this.leadProfile = LeadProfile.empty();
        this.handoffSummary = null;
    }

    public String getCustomerId() {
        return customerId;
    }

    public SessionStatus getStatus() {
        return status;
    }

    /** 将会话转为人工处理；该操作不能恢复自动聊天。 */
    public void escalateToHuman() {
        this.status = SessionStatus.HUMAN_ESCALATED;
    }

    /** 将会话关闭；该操作不能恢复自动聊天。 */
    public void close() {
        this.status = SessionStatus.CLOSED;
    }

    /**
     * 执行人工重新激活，并同时清空连续异常计数。
     *
     * <p>仅允许从 HUMAN_ESCALATED 恢复；聊天处理链路不会调用此方法。</p>
     *
     * @return 是否实际完成了重新激活
     */
    public synchronized boolean reactivateAfterHumanEscalation() {
        if (status != SessionStatus.HUMAN_ESCALATED) {
            return false;
        }

        status = SessionStatus.ACTIVE;
        abnormalCount = 0;
        // 新一轮自动处理不应继续展示上一次人工交接摘要。
        handoffSummary = null;
        return true;
    }

    public int getAbnormalCount() {
        return abnormalCount;
    }

    public void setAbnormalCount(int abnormalCount) {
        this.abnormalCount = abnormalCount;
    }

    public Instant getLastSentAt() {
        return lastSentAt;
    }

    public void setLastSentAt(Instant lastSentAt) {
        this.lastSentAt = lastSentAt;
    }

    /**
     * 保存一条已成功处理的消息，并在超过上限时移除最早消息。
     *
     * @param message 要保存的客户或 Agent 消息
     */
    public synchronized void addConversationMessage(ConversationMessage message) {
        conversationHistory.addLast(message);
        while (conversationHistory.size() > MAX_HISTORY_MESSAGES) {
            conversationHistory.removeFirst();
        }
    }

    /** 返回有限历史的只读快照，避免调用方修改 Session 内部集合。 */
    public synchronized List<ConversationMessage> getConversationHistorySnapshot() {
        return List.copyOf(new ArrayList<>(conversationHistory));
    }

    public LeadProfile getLeadProfile() {
        return leadProfile;
    }

    /** 将本轮明确提取到的初筛字段增量合并到 Session。 */
    public synchronized void mergeLeadQualification(LeadQualificationUpdate update) {
        leadProfile = leadProfile.merge(update);
    }

    public String getHandoffSummary() {
        return handoffSummary;
    }

    /** 保存仅供人工接管时查看的服务端摘要。 */
    public void setHandoffSummary(String handoffSummary) {
        this.handoffSummary = handoffSummary;
    }
}
