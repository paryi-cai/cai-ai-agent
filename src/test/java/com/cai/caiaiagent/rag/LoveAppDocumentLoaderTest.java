package com.cai.caiaiagent.rag;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.util.List;

/**
 * LoveAppDocumentLoader 单元测试
 *
 * 特点：不启动 Spring 容器（手动 new），不调用 AI，纯本地验证切分逻辑 ✅ 快且免费
 * 作用：把"文档到底被切成什么样"打印出来，直观理解切分规则
 */
class LoveAppDocumentLoaderTest {

    @Test
    @DisplayName("加载 Markdown 文档：验证切分数量、元数据、内容完整性")
    void testLoadMarkdowns() {
        // 手动构造（不依赖 Spring）：只需要传一个资源解析器
        LoveAppDocumentLoader loader = new LoveAppDocumentLoader(new PathMatchingResourcePatternResolver());

        List<Document> documents = loader.loadMarkdowns();

        // ===== 打印每个切片，直观查看"按什么切分的" =====
        System.out.println("切片总数：" + documents.size());
        for (int i = 0; i < documents.size(); i++) {
            Document doc = documents.get(i);
            String text = doc.getText();
            System.out.println("========== 切片 " + (i + 1) + " ==========");
            System.out.println("id: " + doc.getId());
            System.out.println("metadata: " + doc.getMetadata());
            System.out.println("内容预览：" + text.substring(0, Math.min(80, text.length())).replace("\n", " ") + "...");
        }

        // ===== 断言（单元测试的核心：把预期固化成检查项） =====
        // 1. 3 篇文档 × 5 组问答 = 15 个切片（每遇到一条 --- 水平线切一刀）
        Assertions.assertEquals(15, documents.size(), "切片总数应为 15");

        // 2. 每个切片都带 filename 元数据（为后续过滤/溯源做准备）
        Assertions.assertTrue(documents.stream().allMatch(d -> d.getMetadata().containsKey("filename")),
                "每个切片都应该有 filename 元数据");

        // 3. 没有空白切片
        Assertions.assertTrue(documents.stream().allMatch(d -> d.getText() != null && !d.getText().isBlank()),
                "不应出现空白切片");

        // 4. 单条问答的完整内容只出现在一个切片中（证明按 --- 正确切开了）
        long count = documents.stream().filter(d -> d.getText().contains("激情期")).count();
        Assertions.assertEquals(1, count, "已婚篇第 1 问应完整出现在且仅出现在一个切片里");
    }

    @Test
    void loadMarkdowns() {
    }
}
