package com.cai.caiaiagent.rag;

import jakarta.annotation.Resource;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.model.transformer.KeywordMetadataEnricher;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 自定义关键词元数据增强器（ETL 的 Transform 阶段示例）
 *
 * 原理：调用 AI 为每个文档提取 N 个关键词，写入文档的 metadata（键：excerpt_keywords）
 * 作用：把关键词作为"多维索引"，检索时可用于元数据过滤、提升匹配质量
 *
 * 注意：每个文档都会调用一次 AI，有额外成本和时间消耗，适合离线灌库时使用
 * （升级提示：1.0 版本包名变更为 org.springframework.ai.model.transformer）
 */
@Component
public class MyKeywordEnricher {

    @Resource
    private ChatModel dashscopeChatModel;

    /**
     * 为文档列表补充关键词元信息
     *
     * @param documents 待增强的文档列表
     * @return 增强后的文档列表（metadata 中多了关键词）
     */
    public List<Document> enrichDocuments(List<Document> documents) {
        KeywordMetadataEnricher enricher = new KeywordMetadataEnricher(this.dashscopeChatModel, 5);
        return enricher.apply(documents);
    }
}
