package com.example.leadagent.model;

/**
 * 当前 Session 已确认的客户初筛资料；未知字段保持为 {@code null}。
 *
 * @param interestedProduct 客户感兴趣的产品或方案
 * @param customerNeed 客户希望解决的问题
 * @param budget 客户主动披露的预算信息
 * @param purchaseTimeline 预计采购或落地时间
 * @param decisionRole 客户在采购决策中的角色
 * @param contactPreference 客户偏好的后续联系方式或联系时间
 */
public record LeadProfile(
        String interestedProduct,
        String customerNeed,
        String budget,
        String purchaseTimeline,
        String decisionRole,
        String contactPreference
) {

    /** 创建一个尚未收集到任何资料的初筛结果。 */
    public static LeadProfile empty() {
        return new LeadProfile(null, null, null, null, null, null);
    }

    /**
     * 只使用本轮明确提取到的非空字段覆盖旧值，避免未知值擦除已有资料。
     *
     * @param update 本轮初筛字段增量
     * @return 合并后的不可变初筛资料
     */
    public LeadProfile merge(LeadQualificationUpdate update) {
        if (update == null) {
            return this;
        }
        return new LeadProfile(
                preferNew(update.interestedProduct(), interestedProduct),
                preferNew(update.customerNeed(), customerNeed),
                preferNew(update.budget(), budget),
                preferNew(update.purchaseTimeline(), purchaseTimeline),
                preferNew(update.decisionRole(), decisionRole),
                preferNew(update.contactPreference(), contactPreference)
        );
    }

    /** 空白的新值代表本轮未确认该字段，因此继续保留旧值。 */
    private static String preferNew(String newValue, String oldValue) {
        return newValue == null || newValue.isBlank() ? oldValue : newValue.trim();
    }
}
