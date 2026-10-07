package com.cai.caiaiagent.rag;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

/**
 * 云向量存储测试（DashScopeCloudStore）
 * 直接对百炼知识库做相似度检索
 */
@SpringBootTest
class DashScopeCloudStoreDemoTest {

    @Resource
    private DashScopeCloudStoreDemo dashScopeCloudStoreDemo;

    @Test
    void testSearch() {
        List<Document> documents = dashScopeCloudStoreDemo.search("婚后关系不亲密怎么办？", 3);
        System.out.println("云端命中：" + documents.size() + " 条");
        documents.forEach(doc -> System.out.println("内容：" + doc.getText()
                .replace("\n", " ").substring(0, Math.min(80, doc.getText().length())) + "..."));
        Assertions.assertNotNull(documents);
    }
}
