package com.cai.caiaiagent.rag;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

/**
 * 查询转换器测试：翻译 + 压缩（每个测试 1 次 AI 调用）
 */
@SpringBootTest
class QueryTransformerDemoTest {

    @Resource
    private QueryTransformerDemo queryTransformerDemo;

    /** 英文查询 → 中文查询 */
    @Test
    void testTranslateQuery() {
        String translated = queryTransformerDemo.translateQuery(
                "how to keep the relationship fresh after marriage?");
        System.out.println("翻译后查询：" + translated);
        Assertions.assertNotNull(translated);
        Assertions.assertFalse(translated.isBlank());
    }

    /** 对话历史 + 追问 → 独立完整的查询 */
    @Test
    void testCompressQuery() {
        List<Message> history = List.of(
                new UserMessage("我是程序员鱼皮，我想让另一半（编程导航）更爱我"),
                new AssistantMessage("首先要了解对方的需求，多表达关心与重视，保持高质量的陪伴。"));
        String compressed = queryTransformerDemo.compressQuery("那具体该怎么做？", history);
        System.out.println("压缩后查询：" + compressed);
        Assertions.assertNotNull(compressed);
        Assertions.assertFalse(compressed.isBlank());
    }
}
