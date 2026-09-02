package com.example.leadagent.service.guard;

/**
 * 对模型生成的自然语言回复进行独立安全审核。
 */
@FunctionalInterface
public interface OutputGuardService {

    /**
     * 判断回复草稿是否可以发送给客户。
     *
     * @param replyDraft 模型生成的候选回复
     * @return {@code true} 表示 safe，{@code false} 表示 unsafe
     */
    boolean isSafe(String replyDraft);
}
