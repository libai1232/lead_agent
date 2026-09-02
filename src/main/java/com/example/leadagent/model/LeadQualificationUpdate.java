package com.example.leadagent.model;

/**
 * LLM 从本轮消息和有限历史中提取出的客户初筛字段增量。
 *
 * <p>该结构不包含 Session 状态、计数器、动作或工具名称，不能控制 Agent。</p>
 *
 * @param interestedProduct 客户明确感兴趣的产品或方案
 * @param customerNeed 客户明确表达的业务需求
 * @param budget 客户明确披露的预算
 * @param purchaseTimeline 客户明确披露的采购时间
 * @param decisionRole 客户明确披露的决策角色
 * @param contactPreference 客户明确披露的联系偏好
 */
public record LeadQualificationUpdate(
        String interestedProduct,
        String customerNeed,
        String budget,
        String purchaseTimeline,
        String decisionRole,
        String contactPreference
) {
}
