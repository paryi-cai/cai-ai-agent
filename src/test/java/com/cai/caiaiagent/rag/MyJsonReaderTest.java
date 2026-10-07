package com.cai.caiaiagent.rag;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.util.List;

/**
 * JSON 文档读取器测试（不启动 Spring、不消耗 API）
 */
class MyJsonReaderTest {

    @Test
    void testLoadJson() throws IOException {
        Resource resource = new PathMatchingResourcePatternResolver()
                .getResource("classpath:document/love-courses.json");
        MyJsonReader myJsonReader = new MyJsonReader();

        // 1. 全量读取
        List<Document> basic = myJsonReader.loadBasicJsonDocuments(resource);
        // 2. 只取指定字段
        List<Document> byFields = myJsonReader.loadJsonWithSpecificFields(resource);
        // 3. JSON Pointer 精确定位 /courses
        List<Document> byPointer = myJsonReader.loadJsonWithPointer(resource);

        System.out.println("全量读取：" + basic.size() + " 个文档");
        System.out.println("指定字段读取：" + byFields.size() + " 个文档");
        System.out.println("Pointer(/courses)读取：" + byPointer.size() + " 个文档");
        System.out.println("===== Pointer 读取内容预览 =====");
        byPointer.forEach(doc -> System.out.println("[" + doc.getText().length() + "字] "
                + doc.getText().replace("\n", " ").substring(0, Math.min(90, doc.getText().length())) + "..."));

        Assertions.assertFalse(basic.isEmpty(), "应该读到文档");
        Assertions.assertFalse(byPointer.isEmpty(), "Pointer 应该定位到 courses 数组");
    }
}
