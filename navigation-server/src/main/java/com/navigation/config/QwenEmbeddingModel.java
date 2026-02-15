package com.navigation.config;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 千问Embedding模型适配器
 * 实现LangChain4J的EmbeddingModel接口，直接调用千问的embedding API
 * 避免OpenAI客户端的兼容性问题
 */
@Slf4j
public class QwenEmbeddingModel implements EmbeddingModel {

    private final String apiKey;
    private final String baseUrl;
    private final String modelName;
    private final RestTemplate restTemplate;

    public QwenEmbeddingModel(String apiKey, String baseUrl, String modelName) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.modelName = modelName;
        this.restTemplate = new RestTemplate();
        log.info("[QwenEmbeddingModel] 初始化 | baseUrl={} | model={}", baseUrl, modelName);
    }

    @Override
    public Response<List<Embedding>> embedAll(List<TextSegment> textSegments) {
        try {
            // 提取文本内容
            List<String> texts = textSegments.stream()
                    .map(TextSegment::text)
                    .collect(Collectors.toList());

            log.info("[QwenEmbeddingModel] 开始向量化 | 文本数量={}", texts.size());

            // 千问API限制: 每批最多10条文本
            int batchSize = 10;
            List<Embedding> allEmbeddings = new ArrayList<>();

            // 分批处理
            for (int i = 0; i < texts.size(); i += batchSize) {
                int end = Math.min(i + batchSize, texts.size());
                List<String> batch = texts.subList(i, end);

                log.info("[QwenEmbeddingModel] 处理批次 | 当前批次={}/{} | 批次大小={}",
                        (i / batchSize) + 1, (texts.size() + batchSize - 1) / batchSize, batch.size());

                // 构建请求
                JSONObject requestBody = new JSONObject();
                requestBody.put("model", modelName);
                requestBody.put("input", batch);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("Authorization", "Bearer " + apiKey);

                HttpEntity<String> entity = new HttpEntity<>(requestBody.toJSONString(), headers);

                // 调用千问API
                String url = baseUrl + "/embeddings";

                log.debug("[QwenEmbeddingModel] 发送请求 | url={} | batchSize={}", url, batch.size());

                ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

                // 解析响应
                JSONObject responseBody = JSON.parseObject(response.getBody());
                JSONArray dataArray = responseBody.getJSONArray("data");

                if (dataArray == null || dataArray.isEmpty()) {
                    throw new RuntimeException("千问API返回空数据: " + response.getBody());
                }

                // 提取向量
                for (int j = 0; j < dataArray.size(); j++) {
                    JSONObject dataItem = dataArray.getJSONObject(j);
                    JSONArray embeddingArray = dataItem.getJSONArray("embedding");

                    float[] vector = new float[embeddingArray.size()];
                    for (int k = 0; k < embeddingArray.size(); k++) {
                        vector[k] = embeddingArray.getFloatValue(k);
                    }

                    allEmbeddings.add(new Embedding(vector));
                }
            }

            log.info("[QwenEmbeddingModel] 向量化成功 | 总文本数量={} | 向量维度={}",
                    allEmbeddings.size(), allEmbeddings.get(0).vector().length);

            return Response.from(allEmbeddings);

        } catch (Exception e) {
            log.error("[QwenEmbeddingModel] 向量化失败 | error={}", e.getMessage(), e);
            throw new RuntimeException("千问embedding调用失败: " + e.getMessage(), e);
        }
    }

    @Override
    public Response<Embedding> embed(TextSegment textSegment) {
        List<TextSegment> segments = new ArrayList<>();
        segments.add(textSegment);
        Response<List<Embedding>> response = embedAll(segments);
        return Response.from(response.content().get(0));
    }

    @Override
    public Response<Embedding> embed(String text) {
        return embed(TextSegment.from(text));
    }
}
