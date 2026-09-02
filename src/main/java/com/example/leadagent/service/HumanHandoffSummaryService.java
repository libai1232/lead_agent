package com.example.leadagent.service;

import com.example.leadagent.model.CustomerSession;
import com.example.leadagent.model.LeadProfile;
import com.example.leadagent.model.LlmDecision;
import org.springframework.stereotype.Service;

/**
 * 使用代码中的已确认字段生成稳定的人工交接摘要，不再发起额外生成式调用。
 */
@Service
public class HumanHandoffSummaryService {

    /**
     * 生成只供人工查看的摘要；未知资料明确标记为“未确认”。
     *
     * @param session 已累计客户资料的 Session
     * @param decision 触发转人工的当前轮决策
     * @return 确定性人工摘要
     */
    public String create(CustomerSession session, LlmDecision decision) {
        LeadProfile profile = session.getLeadProfile();
        // 状态机已更新异常计数，因此可以准确区分 Java 强制升级与模型建议升级。
        String escalationReason = session.getAbnormalCount() >= 2
                ? "连续两次异常，Java 状态机强制转人工"
                : "当前轮结构化决策建议转人工";
        return "转人工原因：%s；当前意图：%s；明显不满：%s；"
                .formatted(escalationReason, decision.intent(), decision.dissatisfied())
                + "感兴趣产品：%s；客户需求：%s；预算：%s；采购时间：%s；决策角色：%s；联系偏好：%s。"
                .formatted(
                        knownOrUnknown(profile.interestedProduct()),
                        knownOrUnknown(profile.customerNeed()),
                        knownOrUnknown(profile.budget()),
                        knownOrUnknown(profile.purchaseTimeline()),
                        knownOrUnknown(profile.decisionRole()),
                        knownOrUnknown(profile.contactPreference())
                );
    }

    /** 避免摘要中的未知字段被误解为空数据。 */
    private String knownOrUnknown(String value) {
        return value == null || value.isBlank() ? "未确认" : value;
    }
}
