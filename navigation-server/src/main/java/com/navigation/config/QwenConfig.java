package com.navigation.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "langchain4j.open-ai.chat-model")
@Data
public class QwenConfig {
    private String apiKey;
    private String baseUrl;
    private String modelName;
    private int timeout = 60000;

    // 提供getApiUrl方法,返回完整的chat completions端点
    public String getApiUrl() {
        return baseUrl + "/chat/completions";
    }
}
