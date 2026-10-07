package com.cai.caiaiagent.rag;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 查询重写测试（1 次 AI 调用）
 */
@SpringBootTest
class QueryRewriterTest {

    @Resource
    private QueryRewriter queryRewriter;

    @Test
    void testDoQueryRewrite() {
        // 故意用一句口语化、含糊的查询
        String original = "结婚之后感觉感情淡了咋整";
        String rewritten = queryRewriter.doQueryRewrite(original);
        System.out.println("原始查询：" + original);
        System.out.println("重写结果：" + rewritten);
        Assertions.assertNotNull(rewritten);
        Assertions.assertFalse(rewritten.isBlank());
    }
}
