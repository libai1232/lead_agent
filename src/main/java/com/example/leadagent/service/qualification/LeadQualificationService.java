package com.example.leadagent.service.qualification;

import com.example.leadagent.model.ConversationMessage;
import com.example.leadagent.model.LeadQualificationUpdate;

import java.util.List;

/**
 * 从不可信客户对话中提取不具备执行权限的初筛字段。
 */
public interface LeadQualificationService {

    /**
     * 提取本轮可以明确确认的初筛资料增量。
     *
     * @param currentMessage 当前客户消息
     * @param history 当前消息之前的有限历史
     * @return 本轮确认的资料；未知字段为空字符串
     */
    LeadQualificationUpdate extract(String currentMessage, List<ConversationMessage> history);
}
