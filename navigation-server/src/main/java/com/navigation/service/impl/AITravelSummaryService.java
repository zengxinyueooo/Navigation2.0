package com.navigation.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.navigation.config.DeepSeekConfig;
import com.navigation.entity.UserRoute;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class AITravelSummaryService {

    @Autowired
    private DeepSeekConfig deepSeekConfig;

    @Autowired
    private UserRouteServiceImpl userRouteService;

    @Autowired
    private UserServiceImpl userService;

    /**
     * 实时生成行程AI摘要（无缓存）
     */
    public String generateTravelSummaryRealTime(Long routeId) {
        // 获取行程数据
        UserRoute route = userRouteService.getRouteById(routeId);
        if (route == null) {
            return generateFallbackSummary();
        }

        try {
            String prompt = buildTravelPrompt(route);
            return callDeepSeekAPI(prompt);
        } catch (Exception e) {
            log.error("实时生成行程摘要失败 routeId: {}", routeId, e);
            return generateFallbackSummary(route);
        }
    }

    /**
     * 构建智能提示词（只使用路线数据）
     */
    private String buildTravelPrompt(UserRoute route) {
        return String.format("""
                        你是一名深受欢迎的旅行博客作者，文风轻松热情，但也很文艺治愈，善于发现旅途中的小确幸。
                                    
                        请为以下行程生成一段生动有趣的旅行摘要（80-120字）：
                                    
                        出发地：%s
                        目的地：%s
                        出行方式：%s
                        出行时间：%s
                                    
                        要求：
                        1. 语言轻松幽默，有画面感，自然亲切，也比较文艺
                        2. 用第一人称"我"来叙述
                        3. 不要提及照片等用户未提供的信息
                        4. 真实性第一！必须严格基于真实的行程数据进行创作，严禁捏造任何数据中不存在的信息。
                        如果数据不足，宁可简化内容，也不要虚构
                        5.请你首先在内心分析这段行程的特点，然后生成最终的行程摘要。
                                    
                        示例风格：
                        "刚刚从%s出发，乘坐%s前往%s，一路上..."
                        """,
                route.getOriginName(),
                route.getDestinationName(),
                route.getTravelMode(),
                formatDate(route.getCreateTime()),
                route.getOriginName(),
                route.getTravelMode(),
                route.getDestinationName()
        );
    }

    /**
     * 调用DeepSeek API
     */
    private String callDeepSeekAPI(String prompt) throws IOException {
        CloseableHttpClient client = HttpClients.createDefault();
        HttpPost post = new HttpPost(deepSeekConfig.getApiUrl());

        post.setHeader("Content-Type", "application/json");
        post.setHeader("Authorization", "Bearer " + deepSeekConfig.getApiKey());

        // 设置超时
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(5000)
                .setSocketTimeout(10000)
                .build();
        post.setConfig(config);

        JSONObject requestBody = new JSONObject();
        requestBody.put("model", "deepseek-chat");

        JSONArray messages = new JSONArray();
        JSONObject message = new JSONObject();
        message.put("role", "user");
        message.put("content", prompt);
        messages.add(message);

        requestBody.put("messages", messages);
        requestBody.put("temperature", 0.8);
        requestBody.put("max_tokens", 300);

        post.setEntity(new StringEntity(requestBody.toJSONString(), StandardCharsets.UTF_8));

        try (CloseableHttpResponse response = client.execute(post)) {
            if (response.getStatusLine().getStatusCode() != 200) {
                throw new IOException("API请求失败: " + response.getStatusLine().getStatusCode());
            }

            String responseString = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            return parseAIResponse(responseString);
        }
    }

    /**
     * 解析AI响应
     */
    private String parseAIResponse(String response) {
        try {
            JSONObject jsonResponse = JSONObject.parseObject(response);
            JSONArray choices = jsonResponse.getJSONArray("choices");

            if (choices != null && !choices.isEmpty()) {
                JSONObject firstChoice = choices.getJSONObject(0);
                JSONObject message = firstChoice.getJSONObject("message");
                return message.getString("content").trim();
            }
        } catch (Exception e) {
            log.error("解析AI响应失败", e);
        }
        return generateFallbackSummary();
    }

    /**
     * 新增：SSE流式生成行程摘要
     */
    public void generateAndStreamSummary(Long routeId, SseEmitter emitter) {
        try {
            // 1. 获取行程数据
            UserRoute route = userRouteService.getRouteById(routeId);
            if (route == null) {
                emitter.send(com.navigation.utils.StreamEventVOBuilder.buildErrorEvent("未找到行程数据"));
                emitter.complete();
                return;
            }

            // 2. 构建提示词
            String prompt = buildTravelPrompt(route);

            // 3. 发送开启事件
            emitter.send(com.navigation.utils.StreamEventVOBuilder.buildOpenEvent());

            // 4. 调用流式DeepSeek API(最复杂的)
            callDeepSeekStreamAPI(prompt, emitter, route);

        } catch (Exception e) {
            log.error("流式生成行程摘要失败 routeId: {}", routeId, e);
            try {
                emitter.send(com.navigation.utils.StreamEventVOBuilder.buildErrorEvent("生成失败: " + e.getMessage()));
            } catch (IOException ex) {
                log.error("发送错误事件失败", ex);
            }
            emitter.complete();
        }
    }

    /**
     * 新增：调用DeepSeek流式API
     */
    private void callDeepSeekStreamAPI(String prompt, SseEmitter emitter, UserRoute route) throws IOException {
        CloseableHttpClient client = HttpClients.createDefault();
        HttpPost post = new HttpPost(deepSeekConfig.getApiUrl());

        post.setHeader("Content-Type", "application/json");
        post.setHeader("Authorization", "Bearer " + deepSeekConfig.getApiKey());
        post.setHeader("Accept", "text/event-stream"); // 重要：请求流式响应

        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(5000)
                .setSocketTimeout(60000) // 流式请求需要更长超时时间
                .build();
        post.setConfig(config);

        JSONObject requestBody = new JSONObject();
        requestBody.put("model", "deepseek-chat");

        JSONArray messages = new JSONArray();
        JSONObject message = new JSONObject();
        message.put("role", "user");
        message.put("content", prompt);
        messages.add(message);

        requestBody.put("messages", messages);
        requestBody.put("temperature", 0.8);
        requestBody.put("max_tokens", 300);
        requestBody.put("stream", true); // 关键：开启流式输出
        //stream: true参数是告诉DeepSeek API：'请用流式方式返回数据，不要一次性返回完整结果'。

        post.setEntity(new StringEntity(requestBody.toJSONString(), StandardCharsets.UTF_8));

        //逐行读取DeepSeek返回的数据流。
        try (CloseableHttpResponse response = client.execute(post)) {
            if (response.getStatusLine().getStatusCode() != 200) {
                throw new IOException("API请求失败: " + response.getStatusLine().getStatusCode());
            }

            // 流式读取响应
            InputStream inputStream = response.getEntity().getContent();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

            String line;
            StringBuilder fullContent = new StringBuilder();
            int messageIndex = 0;  // 消息序号

            while ((line = reader.readLine()) != null) {
                if (line.startsWith("data: ")) { //DeepSeek的流式响应格式是每行以data:开头，后面跟着JSON数据。
                    String data = line.substring(6).trim();

                    if ("[DONE]".equals(data)) { //当读取到[DONE]时，表示AI已经生成完毕
                        // 发送关闭事件
                        emitter.send(com.navigation.utils.StreamEventVOBuilder.buildCloseEvent());
                        break;
                    }

                    if (!data.isEmpty()) {
                        String contentChunk = parseStreamResponse(data); //对于每一块数据，我们解析出其中的文本内容
                        if (contentChunk != null && !contentChunk.isEmpty()) {
                            fullContent.append(contentChunk);  // 累积

                            // 发送累积的完整文本
                            emitter.send(com.navigation.utils.StreamEventVOBuilder.buildMessageEvent(
                                    messageIndex++,
                                    fullContent.toString()));

                            log.debug("[AITravelSummaryService] 发送累积文本 | index={} | length={}",
                                    messageIndex - 1, fullContent.length());
                        }
                    }
                }
            }

            log.info("[AITravelSummaryService] 流式生成完成 | 总字符数={} | 完整内容={}",
                    fullContent.length(), fullContent.toString());
            emitter.complete();

        } catch (Exception e) {
            log.error("流式API调用失败", e);
            throw e;
        }
    }

    /**
     * 新增：解析流式响应
     */
    //从DeepSeek的响应中提取出实际的文本内容,DeepSeek返回的数据结构类似这样：{
    //  "choices": [
    //    {
    //      "delta": {
    //        "content": "刚"
    //      }
    //    }
    //  ]
    //}
    private String parseStreamResponse(String data) {
        try {
            JSONObject jsonObject = JSONObject.parseObject(data);
            JSONArray choices = jsonObject.getJSONArray("choices");
            if (choices != null && !choices.isEmpty()) {
                JSONObject choice = choices.getJSONObject(0);
                JSONObject delta = choice.getJSONObject("delta");
                if (delta != null) {
                    return delta.getString("content"); //提取出content字段的值，这就是AI实时生成的文字片段
                }
            }
        } catch (Exception e) {
            log.error("解析流式响应失败: {}", data, e);
        }
        return "";
    }



    /**
     * 日期格式化
     */
    private String formatDate(Date date) {
        if (date == null) {
            return "某个时间";
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日 HH:mm");
        return sdf.format(date);
    }

    /**
     * 降级方案
     */
    private String generateFallbackSummary(UserRoute route) {
        return String.format("刚刚从%s前往%s，选择了%s出行。这是一段美好的旅程！希望沿途的风景让你心情愉悦！",
                route.getOriginName(),
                route.getDestinationName(),
                route.getTravelMode()
        );
    }

    private String generateFallbackSummary() {
        return "这是一段精彩的旅行！沿途的风景和体验都值得珍藏。希望你在旅途中收获了美好的回忆！";
    }

}