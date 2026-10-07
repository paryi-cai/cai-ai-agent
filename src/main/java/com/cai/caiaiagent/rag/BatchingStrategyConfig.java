package com.cai.caiaiagent.rag;

import com.knuddels.jtokkit.api.EncodingType;
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.embedding.TokenCountBatchingStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 自定义批处理策略配置（对应教程"扩展知识 - 批处理策略"）
 *
 * 背景：一次性嵌入大量文档可能超出模型单次请求的 token 上限，导致报错或性能问题；
 * BatchingStrategy 负责把大批量文档切成合适的小批次。
 *
 * TokenCountBatchingStrategy 参数：
 * - EncodingType：token 编码类型（与模型 tokenizer 对齐，如 CL100K_BASE）
 * - maxInputTokenCount：单个批次的最大 token 数
 * - averageTokenCountReservePercentage：预留比例（避免估算偏差导致超限）
 *
 * 该 Bean 会被下面的向量库 Builder 使用（SimpleVectorStore / PgVectorStore 均已接入）
 */
@Configuration
public class BatchingStrategyConfig {

    @Bean
    public BatchingStrategy customTokenCountBatchingStrategy() {
        return new TokenCountBatchingStrategy(
                EncodingType.CL100K_BASE,   // 指定编码类型
                8000,                        // 单批最大输入 token 数
                0.1);                        // 预留百分比（10% 余量）
    }
}
