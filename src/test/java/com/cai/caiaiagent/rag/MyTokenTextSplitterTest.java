package com.cai.caiaiagent.rag;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.util.List;

/**
 * Token 文本切分器测试（不启动 Spring、不消耗 API）
 * 作用：直观对比不同切分参数的效果，理解"切片粒度"对 RAG 的影响
 */
class MyTokenTextSplitterTest {

    @Test
    @DisplayName("Token 切分：默认参数 vs 自定义参数 vs 激进参数")
    void testSplit() {
        // 准备原始切片（15 个问答）
        LoveAppDocumentLoader loader = new LoveAppDocumentLoader(new PathMatchingResourcePatternResolver());
        List<Document> documents = loader.loadMarkdowns();
        MyTokenTextSplitter splitter = new MyTokenTextSplitter();

        // 1. 默认参数（chunkSize=800 token）
        List<Document> byDefault = splitter.splitDocuments(documents);
        // 2. 自定义参数（chunkSize=200 token）
        List<Document> byCustom = splitter.splitCustomized(documents);
        // 3. 激进参数（chunkSize=60 token）：故意设得很小，观察"语义被切断"
        TokenTextSplitter aggressive = TokenTextSplitter.builder()
                .withChunkSize(60)
                .withMinChunkSizeChars(10)
                .withMinChunkLengthToEmbed(5)
                .withMaxNumChunks(5000)
                .withKeepSeparator(true)
                .build();
        List<Document> byAggressive = aggressive.apply(documents);

        System.out.println("原始切片数：" + documents.size());
        System.out.println("默认参数(800)切分后：" + byDefault.size());
        System.out.println("自定义参数(200)切分后：" + byCustom.size());
        System.out.println("激进参数(60)切分后：" + byAggressive.size());

        // 打印激进切分的片段，观察内容是否被拦腰截断
        System.out.println("===== 激进切分片段预览 =====");
        byAggressive.stream().limit(3).forEach(doc ->
                System.out.println("[" + doc.getText().length() + "字] "
                        + doc.getText().replace("\n", " ").substring(0, Math.min(80, doc.getText().length())) + "..."));

        // 断言：切分不会减少内容块数量（只会拆得更细）；激进切分必然拆出更多块
        Assertions.assertTrue(byDefault.size() >= documents.size(), "切分后的块数不应少于原始块数");
        Assertions.assertTrue(byAggressive.size() > documents.size(), "小 chunkSize 应该拆出更多切片（演示语义切断）");
    }
}
