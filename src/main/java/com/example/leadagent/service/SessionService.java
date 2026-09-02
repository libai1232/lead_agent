package com.example.leadagent.service;

import com.example.leadagent.model.CustomerSession;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 管理进程内的客户 Session。
 *
 * <p>当前实现不包含 Redis、数据库或其他持久化能力。</p>
 */
@Service
public class SessionService {

    /** 以 customerId 为键保存 Session，支持并发获取和创建。 */
    private final Map<String, CustomerSession> sessions = new ConcurrentHashMap<>();

    /**
     * 获取客户已有的 Session；不存在时以默认状态原子创建。
     *
     * @param customerId 客户唯一标识
     * @return 该客户对应的 Session
     */
    public CustomerSession getOrCreate(String customerId) {
        return sessions.computeIfAbsent(customerId, CustomerSession::new);
    }

    /**
     * 模拟人工将已经转人工的 Session 重新激活。
     *
     * <p>不存在的 Session、ACTIVE Session 和 CLOSED Session 均保持原状。</p>
     *
     * @param customerId 要重新激活的客户标识
     * @return 是否实际从 HUMAN_ESCALATED 恢复为 ACTIVE
     */
    public boolean reactivate(String customerId) {
        CustomerSession session = sessions.get(customerId);
        return session != null && session.reactivateAfterHumanEscalation();
    }
}
