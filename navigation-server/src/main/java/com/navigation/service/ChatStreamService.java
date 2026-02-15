package com.navigation.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.navigation.config.QwenConfig;
import com.navigation.tools.AITravelTools;
import com.navigation.vo.ChatMessageVO;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class ChatStreamService {

    @Autowired
    private QwenConfig qwenConfig;

    @Autowired
    private AITravelTools aiTravelTools;

    @Autowired
    private ChatSessionService chatSessionService;

    @Autowired
    private ContentRetriever contentRetriever;

    // System Message常量 - 从ConsultantService复制
    private static final String SYSTEM_MESSAGE_CONTENT = """
        你是"秦游千里"平台提供的专业AI旅游顾问,可以为用户提供以下服务:

        **核心功能:**
        1. 生成陕西省内旅游攻略和行程规划
        2. 查询景点、酒店、美食的详细信息
        3. 解答旅游相关的常见问题

        **详细说明:**

        **1. 旅游攻略生成**
        - 当用户询问行程规划时(如"3天西安怎么玩"、"带孩子的陕西行程"),基于真实数据生成详细的旅行攻略
        - 攻略应包含:每日行程安排、景点推荐、餐饮建议、预算估算
        - 确保推荐的景点、酒店、美食都是陕西省内真实存在的

        **2. 信息查询规则**
        - 查询景点信息需要用户提供准确的景点名称
        - 如果名称不准确或信息不全,请委婉提示用户提供更具体的名称
        - 所有信息必须基于平台真实数据,不能编造不存在的景点或服务

        **3. 数据真实性要求**
        - 所有推荐的景点、酒店、美食必须是陕西省内真实存在的
        - 不能虚构景点信息、开放时间、门票价格等数据
        - 如果不知道某个景点的具体信息,请如实告知用户

        **4. 回复风格要求**
        - 语气友好、专业、热情,体现陕西文化的特色
        - 直接回答问题,不要使用"根据资料显示"、"根据系统信息"等冗余表述
        - 对于旅游相关的问题要详细解答,非旅游问题可以委婉拒绝

        **5. 边界限制**
        - 只回答与陕西旅游、景点查询、行程规划相关的问题
        - 不回答与旅游无关的政治、经济、技术等问题
        - 不提供医疗、法律等专业建议

        你是专业的陕西旅游顾问,请用你的专业知识为用户提供最好的旅游建议和服务!
        """;

    /**
     * 主入口方法 - 流式聊天
     */
    public void streamChat(String sessionId, String message, SseEmitter emitter) {
        try {
            // 0. 发送开启事件
            emitter.send(com.navigation.utils.StreamEventVOBuilder.buildOpenEvent());

            // 1. 获取历史消息(从数据库)
            List<ChatMessageVO> history = chatSessionService.getRecentMessages(sessionId, 20);

            // 2. 构建完整消息列表
            JSONArray messages = buildMessages(history, message);

            // 3. 构建工具定义
            JSONArray tools = buildToolDefinitions();

            // 4. 调用流式API
            String assistantResponse = callDeepSeekStreamWithTools(messages, tools, emitter, sessionId);

            // 5. 保存新消息(只保存到数据库)
            saveMessages(sessionId, message, assistantResponse);

            // 6. 发送关闭事件
            emitter.send(com.navigation.utils.StreamEventVOBuilder.buildCloseEvent());

            // 7. 完成SSE连接
            emitter.complete();

        } catch (Exception e) {
            log.error("[ChatStreamService] 流式聊天失败 | sessionId={} | message={} | error={}",
                sessionId, message, e.getMessage(), e);
            try {
                emitter.send(com.navigation.utils.StreamEventVOBuilder.buildErrorEvent("生成失败: " + e.getMessage()));
                emitter.completeWithError(e);
            } catch (IOException ex) {
                log.error("[ChatStreamService] 发送错误事件失败", ex);
            }
        }
    }

    /**
     * 构建消息列表
     */
    private JSONArray buildMessages(List<ChatMessageVO> history, String userMessage) {
        JSONArray messages = new JSONArray();

        // 1. 添加System Message
        JSONObject systemMsg = new JSONObject();
        systemMsg.put("role", "system");
        systemMsg.put("content", SYSTEM_MESSAGE_CONTENT);
        messages.add(systemMsg);

        // 2. RAG检索相关知识(新增)
        try {
            List<Content> relevantContents = contentRetriever.retrieve(Query.from(userMessage));
            if (!relevantContents.isEmpty()) {
                StringBuilder context = new StringBuilder("以下是相关的背景知识:\n\n");
                for (Content content : relevantContents) {
                    context.append(content.textSegment().text()).append("\n\n");
                }

                // 将检索到的知识作为system消息添加
                JSONObject contextMsg = new JSONObject();
                contextMsg.put("role", "system");
                contextMsg.put("content", context.toString());
                messages.add(contextMsg);

                log.info("[ChatStreamService] RAG检索成功 | 相关文档数={}", relevantContents.size());
            }
        } catch (Exception e) {
            log.warn("[ChatStreamService] RAG检索失败 | error={}", e.getMessage());
            // 检索失败不影响对话,继续执行
        }

        // 3. 转换历史消息 (数据库VO → OpenAI格式)
        if (history != null && !history.isEmpty()) {
            for (ChatMessageVO msg : history) {
                JSONObject historyMsg = new JSONObject();
                historyMsg.put("role", msg.getMessageType());  // "user" 或 "assistant"
                historyMsg.put("content", msg.getContent());
                messages.add(historyMsg);
            }
        }

        // 4. 添加当前用户消息
        JSONObject currentMsg = new JSONObject();
        currentMsg.put("role", "user");
        currentMsg.put("content", userMessage);
        messages.add(currentMsg);

        return messages;
    }

    /**
     * 构建工具定义 - 将4个@Tool方法转换为OpenAI Function Calling格式
     */
    private JSONArray buildToolDefinitions() {
        JSONArray tools = new JSONArray();

        // 工具1: searchScenicSpot
        JSONObject tool1 = new JSONObject();
        tool1.put("type", "function");
        JSONObject func1 = new JSONObject();
        func1.put("name", "searchScenicSpot");
        func1.put("description", "查询景点详细信息,包括介绍、位置、开放时间、门票价格等");
        JSONObject params1 = new JSONObject();
        params1.put("type", "object");
        JSONObject props1 = new JSONObject();
        JSONObject scenicName = new JSONObject();
        scenicName.put("type", "string");
        scenicName.put("description", "景点名称");
        props1.put("scenicName", scenicName);
        params1.put("properties", props1);
        params1.put("required", new JSONArray().fluentAdd("scenicName"));
        func1.put("parameters", params1);
        tool1.put("function", func1);
        tools.add(tool1);

        // 工具2: recommendScenics
        JSONObject tool2 = new JSONObject();
        tool2.put("type", "function");
        JSONObject func2 = new JSONObject();
        func2.put("name", "recommendScenics");
        func2.put("description", "根据地区推荐合适的景点");
        JSONObject params2 = new JSONObject();
        params2.put("type", "object");
        JSONObject props2 = new JSONObject();
        JSONObject regionName = new JSONObject();
        regionName.put("type", "string");
        regionName.put("description", "地区名称");
        props2.put("regionName", regionName);
        params2.put("properties", props2);
        params2.put("required", new JSONArray().fluentAdd("regionName"));
        func2.put("parameters", params2);
        tool2.put("function", func2);
        tools.add(tool2);

        // 工具3: searchHotels
        JSONObject tool3 = new JSONObject();
        tool3.put("type", "function");
        JSONObject func3 = new JSONObject();
        func3.put("name", "searchHotels");
        func3.put("description", "查询酒店信息,可以输入酒店名或地区");
        JSONObject params3 = new JSONObject();
        params3.put("type", "object");
        JSONObject props3 = new JSONObject();
        JSONObject query = new JSONObject();
        query.put("type", "string");
        query.put("description", "酒店名称或地区");
        props3.put("query", query);
        params3.put("properties", props3);
        params3.put("required", new JSONArray().fluentAdd("query"));
        func3.put("parameters", params3);
        tool3.put("function", func3);
        tools.add(tool3);

        // 工具4: recommendFoods
        JSONObject tool4 = new JSONObject();
        tool4.put("type", "function");
        JSONObject func4 = new JSONObject();
        func4.put("name", "recommendFoods");
        func4.put("description", "推荐当地特色美食");
        JSONObject params4 = new JSONObject();
        params4.put("type", "object");
        JSONObject props4 = new JSONObject();
        JSONObject region = new JSONObject();
        region.put("type", "string");
        region.put("description", "地区名称");
        props4.put("region", region);
        params4.put("properties", props4);
        params4.put("required", new JSONArray().fluentAdd("region"));
        func4.put("parameters", params4);
        tool4.put("function", func4);
        tools.add(tool4);

        return tools;
    }

    /**
     * 流式API调用 - 参考AITravelSummaryService的实现
     */
    private String callDeepSeekStreamWithTools(JSONArray messages, JSONArray tools, SseEmitter emitter, String sessionId) throws IOException {
        CloseableHttpClient client = HttpClients.createDefault();
        HttpPost post = new HttpPost(qwenConfig.getApiUrl());

        // 设置请求头
        post.setHeader("Content-Type", "application/json");
        post.setHeader("Authorization", "Bearer " + qwenConfig.getApiKey());
        post.setHeader("Accept", "text/event-stream");

        // 构建请求体
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", qwenConfig.getModelName());
        requestBody.put("messages", messages);
        requestBody.put("tools", tools);
        requestBody.put("stream", true);
        requestBody.put("temperature", 0.7);

        post.setEntity(new StringEntity(requestBody.toJSONString(), StandardCharsets.UTF_8));

        log.info("[ChatStreamService] 调用千问API | model={} | messages={} | url={}",
                qwenConfig.getModelName(), messages.size(), qwenConfig.getApiUrl());
        log.debug("[ChatStreamService] 请求体 | body={}", requestBody.toJSONString());

        CloseableHttpResponse response = client.execute(post);
        int statusCode = response.getStatusLine().getStatusCode();
        log.info("[ChatStreamService] API响应状态 | statusCode={} | statusLine={}",
                statusCode, response.getStatusLine().getReasonPhrase());

        if (statusCode != 200) {
            // 读取错误响应
            BufferedReader errorReader = new BufferedReader(
                new InputStreamReader(response.getEntity().getContent(), StandardCharsets.UTF_8)
            );
            StringBuilder errorBody = new StringBuilder();
            String errorLine;
            while ((errorLine = errorReader.readLine()) != null) {
                errorBody.append(errorLine);
            }
            errorReader.close();
            log.error("[ChatStreamService] API调用失败 | statusCode={} | errorBody={}",
                    statusCode, errorBody.toString());
            throw new IOException("API调用失败: " + statusCode + " - " + errorBody.toString());
        }

        BufferedReader reader = new BufferedReader(
            new InputStreamReader(response.getEntity().getContent(), StandardCharsets.UTF_8)
        );

        StringBuilder fullResponse = new StringBuilder();
        List<JSONObject> toolCallsList = new ArrayList<>();
        String currentToolCallId = null;
        StringBuilder currentToolName = new StringBuilder();
        StringBuilder currentToolArgs = new StringBuilder();
        int messageIndex = 0;  // 消息序号

        String line;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("data: ")) {
                String data = line.substring(6).trim();

                if ("[DONE]".equals(data)) {
                    break;
                }

                try {
                    log.debug("[ChatStreamService] 收到SSE数据 | data={}", data);
                    JSONObject jsonData = JSON.parseObject(data);
                    JSONArray choices = jsonData.getJSONArray("choices");
                    if (choices == null || choices.isEmpty()) {
                        log.debug("[ChatStreamService] choices为空");
                        continue;
                    }

                    JSONObject choice = choices.getJSONObject(0);
                    JSONObject delta = choice.getJSONObject("delta");
                    if (delta == null) {
                        log.debug("[ChatStreamService] delta为空");
                        continue;
                    }

                    log.debug("[ChatStreamService] delta内容 | delta={}", delta.toJSONString());

                    // 处理文本内容
                    String content = delta.getString("content");
                    if (content != null && !content.isEmpty()) {
                        fullResponse.append(content);

                        // 发送累积的完整文本
                        emitter.send(com.navigation.utils.StreamEventVOBuilder.buildMessageEvent(
                                messageIndex++,
                                fullResponse.toString()));

                        log.debug("[ChatStreamService] 发送累积文本 | index={} | length={}",
                                messageIndex - 1, fullResponse.length());
                    }

                    // 处理工具调用
                    JSONArray toolCalls = delta.getJSONArray("tool_calls");
                    if (toolCalls != null && !toolCalls.isEmpty()) {
                        for (int i = 0; i < toolCalls.size(); i++) {
                            JSONObject toolCall = toolCalls.getJSONObject(i);

                            // 获取tool call id
                            String id = toolCall.getString("id");
                            if (id != null) {
                                currentToolCallId = id;
                            }

                            // 获取function信息
                            JSONObject function = toolCall.getJSONObject("function");
                            if (function != null) {
                                String name = function.getString("name");
                                if (name != null) {
                                    currentToolName.append(name);
                                }

                                String arguments = function.getString("arguments");
                                if (arguments != null) {
                                    currentToolArgs.append(arguments);
                                }
                            }
                        }
                    }

                } catch (Exception e) {
                    log.error("[ChatStreamService] 解析SSE数据失败 | data={}", data, e);
                }
            }
        }

        reader.close();
        response.close();
        client.close();

        // 如果有工具调用,处理工具调用
        if (currentToolCallId != null && currentToolName.length() > 0) {
            JSONObject toolCallObj = new JSONObject();
            toolCallObj.put("id", currentToolCallId);
            toolCallObj.put("type", "function");
            JSONObject funcObj = new JSONObject();
            funcObj.put("name", currentToolName.toString());
            funcObj.put("arguments", currentToolArgs.toString());
            toolCallObj.put("function", funcObj);
            toolCallsList.add(toolCallObj);

            log.info("[ChatStreamService] 检测到工具调用 | tool={} | args={}",
                currentToolName.toString(), currentToolArgs.toString());

            // 工具调用静默执行,不发送提示
            log.debug("[ChatStreamService] 工具调用 | tool={}", currentToolName.toString());

            // 处理工具调用并继续
            return handleToolCallsAndContinue(messages, toolCallsList, emitter, sessionId);
        }

        log.info("[ChatStreamService] 流式生成完成 | 总字符数={} | 完整内容={}",
                fullResponse.length(), fullResponse.toString());
        return fullResponse.toString();
    }

    /**
     * 处理工具调用并继续对话
     */
    private String handleToolCallsAndContinue(JSONArray messages, List<JSONObject> toolCalls, SseEmitter emitter, String sessionId) throws IOException {
        // 1. 执行所有工具
        List<JSONObject> toolResults = new ArrayList<>();
        for (JSONObject toolCall : toolCalls) {
            String toolCallId = toolCall.getString("id");
            JSONObject function = toolCall.getJSONObject("function");
            String toolName = function.getString("name");
            String argumentsJson = function.getString("arguments");

            // 执行工具
            String result = executeToolCall(toolName, argumentsJson);

            // 保存工具调用记录到数据库
            try {
                chatSessionService.saveToolCall(sessionId, toolName, result);
            } catch (Exception e) {
                log.error("[ChatStreamService] 保存工具调用失败 | sessionId={} | tool={}", sessionId, toolName, e);
            }

            // 构建工具结果消息
            JSONObject toolResultMsg = new JSONObject();
            toolResultMsg.put("role", "tool");
            toolResultMsg.put("tool_call_id", toolCallId);
            toolResultMsg.put("content", result);
            toolResults.add(toolResultMsg);

            log.info("[ChatStreamService] 工具执行完成 | tool={} | result={}", toolName, result);
        }

        // 2. 添加assistant消息(含tool_calls)
        JSONObject assistantMsg = new JSONObject();
        assistantMsg.put("role", "assistant");
        assistantMsg.put("content", "");
        assistantMsg.put("tool_calls", new JSONArray().fluentAddAll(toolCalls));
        messages.add(assistantMsg);

        // 3. 添加tool结果
        messages.addAll(toolResults);

        // 4. 重新构建工具定义
        JSONArray tools = buildToolDefinitions();

        // 5. 递归调用API获取最终回复
        return callDeepSeekStreamWithTools(messages, tools, emitter, sessionId);
    }

    /**
     * 执行具体工具
     */
    private String executeToolCall(String toolName, String argumentsJson) {
        try {
            log.info("[ChatStreamService] 开始执行工具 | tool={} | args={} | aiTravelTools={}",
                toolName, argumentsJson, (aiTravelTools != null ? "已注入" : "NULL"));

            JSONObject args = JSON.parseObject(argumentsJson);

            switch (toolName) {
                case "searchScenicSpot":
                    String scenicName = args.getString("scenicName");
                    return aiTravelTools.searchScenicSpot(scenicName);

                case "recommendScenics":
                    String regionName = args.getString("regionName");
                    return aiTravelTools.recommendScenics(regionName);

                case "searchHotels":
                    String query = args.getString("query");
                    return aiTravelTools.searchHotels(query);

                case "recommendFoods":
                    String region = args.getString("region");
                    return aiTravelTools.recommendFoods(region);

                default:
                    return "未知工具: " + toolName;
            }
        } catch (Exception e) {
            log.error("[ChatStreamService] 工具执行失败 | tool={} | args={} | error={}",
                toolName, argumentsJson, e.getMessage(), e);
            return "工具执行失败: " + e.getMessage();
        }
    }

    /**
     * 保存会话消息
     */
    private void saveMessages(String sessionId, String userMessage, String assistantResponse) {
        try {
            // 1. 保存用户消息
            chatSessionService.saveUserMessage(sessionId, userMessage);

            // 2. 保存AI消息
            chatSessionService.saveAssistantMessage(sessionId, assistantResponse);

            // 3. 检查是否是首条消息,更新会话名称
            List<ChatMessageVO> history = chatSessionService.getRecentMessages(sessionId, 1);
            if (history.size() <= 2) {  // 只有刚保存的user和assistant消息
                chatSessionService.updateSessionName(sessionId, userMessage);
            }

            log.info("[ChatStreamService] 会话已保存 | sessionId={}", sessionId);
        } catch (Exception e) {
            log.error("[ChatStreamService] 保存消息到数据库失败 | sessionId={} | error={}", sessionId, e.getMessage(), e);
            throw new RuntimeException("保存消息失败", e);
        }
    }
}
