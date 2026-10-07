package com.cai.caiaiagent.rag;

import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;

/**
 * 自定义空上下文查询增强器工厂
 *
 * 背景：检索不到任何相关文档时（空上下文），默认行为是生硬地让 AI 拒绝回答。
 * 通过 ContextualQueryAugmenter 自定义"空上下文提示模板"，可以让 AI 输出更友好的兜底话术。
 */
public class LoveAppContextualQueryAugmenterFactory {

    /** 工具类，禁止实例化 */
    private LoveAppContextualQueryAugmenterFactory() {
    }

    /**
     * 创建自定义的上下文查询增强器
     */
    public static ContextualQueryAugmenter createInstance() {
        // 空上下文时使用的提示模板：告诉模型该怎么回复（模型会按这个要求输出友好提示）
        PromptTemplate emptyContextPromptTemplate = new PromptTemplate("""
                你应该输出下面的内容：
                抱歉，我只能回答恋爱相关的问题，别的没办法帮到您哦，
                有问题可以联系编程导航客服 https://codefather.cn
                """);
        return ContextualQueryAugmenter.builder()
                // 不允许"空上下文正常回答"：检索不到内容时走上面的兜底模板
                .allowEmptyContext(false)
                .emptyContextPromptTemplate(emptyContextPromptTemplate)
                .build();
    }
}
