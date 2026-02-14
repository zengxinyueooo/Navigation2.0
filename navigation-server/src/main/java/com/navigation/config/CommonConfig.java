package com.navigation.config;


import dev.langchain4j.store.embedding.redis.RedisEmbeddingStore;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

@Configuration
public class CommonConfig {

    @Value("${langchain4j.open-ai.chat-model.api-key}")
    private String apiKey;

    @Value("${langchain4j.open-ai.chat-model.base-url}")
    private String baseUrl;

    @Value("${langchain4j.open-ai.chat-model.model-name}")
    private String chatModelName;

    @Value("${langchain4j.open-ai.embedding-model.model-name}")
    private String embeddingModelName;

    @Autowired
    private ChatMemoryStore redisChatMemoryStore;//将会话记录持久化存储到外部的存储器中

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

    // 手动创建EmbeddingModel bean
    @Bean
    public EmbeddingModel embeddingModel() {
        return OpenAiEmbeddingModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(embeddingModelName)
                .timeout(Duration.ofSeconds(60))
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    // 手动创建RedisEmbeddingStore bean
    // 注意: 需要Redis服务器安装RediSearch模块才能使用RedisEmbeddingStore
    // 暂时使用InMemoryEmbeddingStore代替,避免Redis模块依赖问题
    @Bean
    public EmbeddingStore embeddingStoreImpl() {
        // 使用内存存储,不需要Redis RediSearch模块
        return new InMemoryEmbeddingStore();

        // 如果Redis服务器已安装RediSearch模块,可以使用下面的配置:
        // return RedisEmbeddingStore.builder()
        //         .host("47.96.179.70")
        //         .port(8109)
        //         .password("~gmb7GK%aviH!518aU%8")
        //         .dimension(1024)  // DeepSeek text-embedding-v3 的向量维度是1024
        //         .build();
    }


    //测试环境：构建会话记忆对象
    //单一会话记忆该如何存储消息（窗口式、最多存 20 条）
    //弊端：无唯一标识，无法区分多会话；内存存储，无法持久化和分布式共享；灵活性差，无法针对不同会话定制配置
    @Bean
    public ChatMemory chatMemory(){
        MessageWindowChatMemory memory = MessageWindowChatMemory.builder()
                .maxMessages(20)
                .build();
        return memory;
    }

    //实际生产：构建ChatMemoryProvider对象
    //如何为不同的会话（用 memoryId 区分）分配 / 生成对应的记忆，且这些记忆会持久化到 Redis，
    //保证多轮对话的上下文能跨请求、甚至跨服务共享
    @Bean
    public ChatMemoryProvider chatMemoryProvider(){
        ChatMemoryProvider chatMemoryProvider = new ChatMemoryProvider() {
            @Override
            public ChatMemory get(Object memoryId) { //id如果匹配到就复用，如果没有匹配到就调用ChatMemoryProvider对象的get方法获取一个新的使用
                return MessageWindowChatMemory.builder()
                        .id(memoryId)
                        .maxMessages(20)
                        .chatMemoryStore(redisChatMemoryStore)//配置ChatMemoryStore
                        .build();
            }
        };
        return chatMemoryProvider;
    }

    /**
     * 功能：初始化向量数据库，将纯文本格式的文件处理后存入 Redis。
     * 关键步骤：
     * 加载文档：从项目 resources/content 目录纯文本格式的文件，解析为 Document 对象（包含文本内容）。
     * 分割文档：长文档按固定长度（500 字符）分割成短片段，保留 100 字符重叠，避免破坏句子语义。
     * 向量化与存储：通过 EmbeddingStoreIngestor 自动完成：
     * 用 embeddingModel 将文本片段转换为向量（数值数组）。
     * 将向量和原文片段存入 redisEmbeddingStore（即 Redis 中）。
     * @return
     */

    //构建向量数据库操作对象EmbeddingStore
    @Bean
    public EmbeddingStore store(EmbeddingStore embeddingStoreImpl){//embeddingStore的对象, 这个对象的名字不能重复,所以这里使用store
        //1.加载文档进内存
        // TODO: 需要创建 resources/content 目录并添加文档文件
        // List<Document> documents = FileSystemDocumentLoader.loadDocuments("src/main/resources/content");

        //构建文档分割器对象，两个参数是指：每个片段最大容纳的字符，两个片段之间重叠字符的个数
        // DocumentSplitter ds = DocumentSplitters.recursive(500,100);

        //3.构建一个EmbeddingStoreIngestor对象,就是向量存储处理器，完成文本数据切割,向量化, 存储
        // EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
        //         .embeddingStore(embeddingStoreImpl)
        //         .documentSplitter(ds)//使用哪个文本分割器
        //         .embeddingModel(embeddingModel)//存储时候用，向量模型的作用是把分割后的文本片段向量化或者把用户消息向量化
        //         .build();

        //执行处理：将文档切割成片段 -> 用模型转换为向量 -> 存储到 Redis
        // ingestor.ingest(documents); //把需要存储数据的文档对象documents给它传递进去

        // 暂时直接返回 embeddingStoreImpl，文档加载功能待后续添加
        return embeddingStoreImpl;  // 返回向量存储实例，供其他组件使用
    }

    /**
     * 功能：创建一个检索器，用于从 Redis 向量库中查询与 "用户输入" 最相似的文档片段。
     *      逻辑：
     *      当用户输入查询文本时，embeddingModel 会将查询文本转换为向量。
     *      检索器在 redisEmbeddingStore 中计算查询向量与所有存储向量的相似度（如余弦相似度）。
     *      返回相似度 ≥0.5 且 top 3 的文档片段，供大模型生成回答时参考（增强回答的准确性）。
     * @param store
     * @return
     */

    //构建向量数据库检索对象
    @Bean
    public ContentRetriever contentRetriever(EmbeddingStore store, EmbeddingModel embeddingModel){
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(store)//设置向量数据库操作对象
                .minScore(0.5)//设置最小分数，检索阈值：只返回相似度 ≥0.5 的结果
                .maxResults(3)//设置最大片段数量，最多返回 3 条最相似的结果
                .embeddingModel(embeddingModel)//检索时候用，用相同的向量化模型生成查询向量
                .build();
    }

}
