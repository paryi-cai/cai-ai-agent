package com.cai.caiaiagent.rag;

import jakarta.annotation.Resource;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.model.transformer.SummaryMetadataEnricher;
import org.springframework.ai.model.transformer.SummaryMetadataEnricher.SummaryType;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 自定义摘要元数据增强器（ETL 的 Transform 阶段示例）
 *
 * 原理：调用 AI 为文档生成摘要，写入 metadata（键：section_summary）
 * - CURRENT：只生成当前文档摘要
 * - PREVIOUS / NEXT：还会参考前一个/后一个相邻文档，让摘要更完整（成本更高）
 *
 * 用途：摘要可作为"多维索引"，检索时辅助判断文档相关性
 */
@Component
public class MySummaryEnricher {

    @Resource
    private ChatModel dashscopeChatModel;

    /** 只生成当前文档的摘要（每篇 1 次 AI 调用） */
    public List<Document> enrichBySummary(List<Document> documents) {
        SummaryMetadataEnricher enricher = new SummaryMetadataEnricher(this.dashscopeChatModel,
                List.of(SummaryType.CURRENT));
        return enricher.apply(documents);
    }

    /** 关联前后相邻文档生成更完整的摘要（每篇最多 3 次 AI 调用，适合离线批量处理） */
    public List<Document> enrichBySummaryWithNeighbors(List<Document> documents) {
        SummaryMetadataEnricher enricher = new SummaryMetadataEnricher(this.dashscopeChatModel,
                List.of(SummaryType.PREVIOUS, SummaryType.CURRENT, SummaryType.NEXT));
        return enricher.apply(documents);
    }
}
