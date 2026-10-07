package com.cai.caiaiagent.rag;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.JsonReader;
import org.springframework.core.io.Resource;

import java.util.List;

/**
 * JSON 文档读取器演示（ETL 的 Extract 阶段）
 *
 * 演示 JsonReader 的三种用法：
 * 1. 全量读取：把整个 JSON 转为 Document 列表
 * 2. 指定字段：只把指定 JSON 字段作为文档内容（避免噪音）
 * 3. JSON Pointer：用 /courses 这样的路径精确定位要提取的内容
 */
public class MyJsonReader {

    /** 基本用法：把整个 JSON 文档读取为 Document 列表 */
    public List<Document> loadBasicJsonDocuments(Resource resource) {
        JsonReader jsonReader = new JsonReader(resource);
        return jsonReader.get();
    }

    /** 指定使用哪些 JSON 字段作为文档内容 */
    public List<Document> loadJsonWithSpecificFields(Resource resource) {
        JsonReader jsonReader = new JsonReader(resource, "description", "features");
        return jsonReader.get();
    }

    /** 使用 JSON Pointer 精确提取文档内容（/courses 表示提取 courses 数组内的内容） */
    public List<Document> loadJsonWithPointer(Resource resource) {
        JsonReader jsonReader = new JsonReader(resource);
        return jsonReader.get("/courses");
    }
}
