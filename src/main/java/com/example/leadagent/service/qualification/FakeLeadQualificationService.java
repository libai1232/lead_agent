package com.example.leadagent.service.qualification;

import com.example.leadagent.model.ConversationMessage;
import com.example.leadagent.model.LeadQualificationUpdate;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;

/** Fake Profile 使用的固定初筛实现，不调用真实 LLM。 */
@Service
@Profile("fake")
public class FakeLeadQualificationService implements LeadQualificationService {

    /** 返回固定模拟资料，用于跑通 HTTP 和前端展示链路。 */
    @Override
    public LeadQualificationUpdate extract(
            String currentMessage,
            List<ConversationMessage> history
    ) {
        return new LeadQualificationUpdate(
                "模拟产品",
                "了解产品能力",
                "",
                "",
                "",
                ""
        );
    }
}
