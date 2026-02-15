package com.navigation.config;


import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import dev.langchain4j.store.embedding.redis.RedisEmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

@Slf4j
@Configuration
public class CommonConfig {

    @Value("${langchain4j.open-ai.chat-model.api-key}")
    private String apiKey;

    @Value("${langchain4j.open-ai.chat-model.base-url}")
    private String baseUrl;

    @Value("${langchain4j.open-ai.chat-model.model-name}")
    private String chatModelName;

    @Value("${langchain4j.open-ai.embedding-model.base-url}")
    private String embeddingBaseUrl;

    @Value("${langchain4j.open-ai.embedding-model.model-name}")
    private String embeddingModelName;

    // 手动创建OpenAiChatModel bean
    @Bean
    public OpenAiChatModel openAiChatModel() {
        return OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(chatModelName)
                .timeout(Duration.ofSeconds(60))
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    // 手动创建OpenAiStreamingChatModel bean
    @Bean
    public OpenAiStreamingChatModel openAiStreamingChatModel() {
        return OpenAiStreamingChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(chatModelName)
                .timeout(Duration.ofSeconds(60))
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    // 手动创建EmbeddingModel bean - 使用自定义千问实现
    @Bean
    public EmbeddingModel embeddingModel() {
        log.info("[CommonConfig] 初始化千问Embedding模型(自定义实现) | baseUrl={} | model={}", embeddingBaseUrl, embeddingModelName);
        return new QwenEmbeddingModel(apiKey, embeddingBaseUrl, embeddingModelName);
    }

    // 手动创建RedisEmbeddingStore bean
    // 注意: 需要Redis服务器安装RediSearch模块才能使用RedisEmbeddingStore
    @Bean
    public EmbeddingStore<TextSegment> embeddingStoreImpl() {
        log.info("[CommonConfig] 初始化Redis向量存储 | host=47.96.179.70 | port=8110 | dimension=1024");

        try {
            return RedisEmbeddingStore.builder()
                    .host("47.96.179.70")
                    .port(8110)
                    .password("~gmb7GK%aviH!518aU%8")
                    .dimension(1024)  // 千问 text-embedding-v3 的向量维度是1024
                    .indexName("navigation-embeddings-v3")  // 修改索引名称,避免与旧索引冲突
                    .build();
        } catch (Exception e) {
            log.error("[CommonConfig] Redis向量存储初始化失败,降级为内存存储 | error={}", e.getMessage());
            return new InMemoryEmbeddingStore<>();  // 降级方案
        }
    }


    // 注意：ChatMemory 和 ChatMemoryProvider 已移除
    // 现在使用数据库直接管理聊天历史，通过 ChatSessionService 实现
    // 注意：store() Bean已移除，直接使用embeddingStoreImpl

    /**
     * 功能：创建一个检索器，用于从 Redis 向量库中查询与 "用户输入" 最相似的文档片段。
     *      逻辑：
     *      当用户输入查询文本时，embeddingModel 会将查询文本转换为向量。
     *      检索器在 redisEmbeddingStore 中计算查询向量与所有存储向量的相似度（如余弦相似度）。
     *      返回相似度 ≥0.5 且 top 3 的文档片段，供大模型生成回答时参考（增强回答的准确性）。
     * @param
     * @return
     */

    //构建向量数据库检索对象
    @Bean
    public ContentRetriever contentRetriever(EmbeddingStore<TextSegment> embeddingStoreImpl, EmbeddingModel embeddingModel){
        log.info("[CommonConfig] 初始化向量检索器 | minScore=0.6 | maxResults=5");

        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStoreImpl)//设置向量数据库操作对象
                .embeddingModel(embeddingModel)//检索时候用，用相同的向量化模型生成查询向量
                .minScore(0.6)//提高相似度阈值(0.5→0.6)
                .maxResults(5)//增加返回结果数(3→5)
                .build();
    }

    //构建文档加载器对象
    @Bean
    public EmbeddingStoreIngestor embeddingStoreIngestor(
            EmbeddingStore<TextSegment> embeddingStore,
            EmbeddingModel embeddingModel) {

        log.info("[CommonConfig] 初始化文档加载器 | chunkSize=500 | overlap=100");

        return EmbeddingStoreIngestor.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .documentSplitter(DocumentSplitters.recursive(
                    500,   // 每个文本块的字符数
                    100    // 块之间的重叠字符数
                ))
                .build();
    }

}
