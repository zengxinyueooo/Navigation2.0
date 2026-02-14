package com.navigation.initializer;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 向量数据库初始化器
 * 应用启动时自动加载rag.txt知识库到向量数据库
 */
@Component
@Slf4j
public class VectorStoreInitializer implements ApplicationRunner {

    @Autowired
    private EmbeddingStoreIngestor ingestor;

    @Autowired
    private EmbeddingStore<TextSegment> embeddingStore;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("[VectorStoreInitializer] 开始加载知识库...");

        try {
            // 读取rag.txt
            ClassPathResource resource = new ClassPathResource("content/rag.txt");
            String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

            // 创建文档并加载到向量库
            Document document = Document.from(content);
            ingestor.ingest(document);

            log.info("[VectorStoreInitializer] 知识库加载成功 | 文件=rag.txt | 大小={}字节", content.length());
        } catch (Exception e) {
            log.error("[VectorStoreInitializer] 知识库加载失败,RAG功能可能受影响", e);
            // 不抛出异常,避免影响应用启动
        }
    }
}
