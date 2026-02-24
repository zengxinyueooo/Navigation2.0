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
import java.util.Map;

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

    @Autowired
    private TravelPlanExtractService travelPlanExtractService;

    // System Message常量 - 从ConsultantService复制
    private static final String SYSTEM_MESSAGE_CONTENT = """
        你是"秦游千里"平台提供的专业AI旅游顾问,可以为用户提供以下服务:

        🚨🚨🚨 **【超级重要 - 回答风格要求】** 🚨🚨🚨
        你必须用轻松、自然、像朋友聊天的语气回答，绝对不能用正式的、官方的、客服式的语气！
        绝对不能说"您好！刚刚通过官方工具实时查询"这种话！
        绝对不能过度使用✅、📌、🔹等符号！
        要用"你"而不是"您"！要像朋友一样说话！

        ⚠️ **【强制规则 - 必须严格遵守】** ⚠️

        **工具调用规则（优先级最高）:**
        1. 当用户询问具体景点的信息时（如门票价格、开放时间、位置、介绍等），你**必须立即调用** searchScenicSpot 工具
           - 示例问题："大唐芙蓉园门票多少钱"、"兵马俑几点开门"、"华山在哪里"
           - 不要凭记忆回答，不要猜测，必须调用工具获取最新准确数据

        2. 当用户询问某地区有哪些景点或推荐景点时，你**必须立即调用** recommendScenics 工具
           - 示例问题："西安有什么好玩的"、"推荐咸阳的景点"、"陕西有什么景点"
           - **特别注意**：当用户问"陕西有什么景点"时，直接传"陕西"给工具，工具会自动查询陕西所有分区（西安、咸阳、宝鸡等）的景点
           - 不要自己判断应该查哪个城市，直接把用户说的地区名传给工具即可

        3. 当用户询问酒店信息时，你**必须立即调用** searchHotels 工具
           - 示例问题："西安附近的酒店"、"大唐不夜城附近住哪"

        4. 当用户询问美食推荐时，你**必须立即调用** recommendFoods 工具
           - 示例问题："西安有什么好吃的"、"推荐延安美食"

        **禁止行为:**
        - ❌ 禁止凭记忆或知识库直接回答门票价格、开放时间等具体信息
        - ❌ 禁止说"我不确定"、"可能是XX元"等模糊回答
        - ❌ 禁止在不调用工具的情况下回答具体景点、酒店、美食的详细信息

        **正确流程:**
        1. 识别用户问题类型
        2. 立即调用对应工具获取数据
        3. 基于工具返回的真实数据组织回答
        4. 用友好的语气呈现给用户

        **工具调用失败或无数据时的处理:**
        - ❌ 绝对不能说："工具没调通"、"系统说暂无"、"工具返回失败"
        - ❌ 绝对不能说："估计是工具没调通，别急"、"数据获取失败"
        - ✅ 正确表达："这方面的信息暂时还没有更新"、"这个地方的数据还在完善中"
        - ✅ 正确表达："暂时没有找到相关信息，可以试试其他地区"
        - ✅ 要像朋友一样自然地说，不要暴露任何技术细节

        **核心功能:**
        1. 生成陕西省内旅游攻略和行程规划
        2. 查询景点、酒店、美食的详细信息（必须通过工具）
        3. 解答旅游相关的常见问题

        **详细说明:**

        **1. 旅游攻略生成**
        - 当用户询问行程规划时(如"3天西安怎么玩"、"带孩子的陕西行程"),基于真实数据生成详细的旅行攻略
        - 攻略应包含:每日行程安排、景点推荐、餐饮建议、预算估算
        - 确保推荐的景点、酒店、美食都是通过工具查询得到的真实数据

        **2. 信息查询规则**
        - 查询景点信息需要用户提供准确的景点名称
        - 如果名称不准确或信息不全,请委婉提示用户提供更具体的名称
        - 所有信息必须通过工具调用获取,不能编造不存在的景点或服务

        **3. 数据真实性要求**
        - 所有推荐的景点、酒店、美食必须通过工具查询获得
        - 不能虚构景点信息、开放时间、门票价格等数据
        - 必须使用工具返回的最新数据

        **4. 回复风格要求（必须严格遵守，否则视为失败）**

        ⚠️ **绝对禁止的表达（一旦出现立即判定为错误）**：
        - ❌ "您好！刚刚通过官方工具实时查询"
        - ❌ "已获取最新、最权威的信息"
        - ❌ "根据资料显示"、"根据系统信息"
        - ❌ "通过工具查询"、"我调用了XX工具"
        - ❌ "刚帮你查了"、"帮你查到了"、"查询到"等暗示查询过程的词
        - ❌ "硬核宝藏清单"、"实时数据"、"最新信息"等暗示数据来源的词
        - ❌ 过度使用"✅"、"📌"、"🔹"等符号（超过2个就算过度）
        - ❌ 使用"您"而不是"你"
        - ❌ 说"为您服务"、"立刻为您安排"等客服话术
        - ❌ "工具没调通"、"系统说"、"工具返回"等暴露技术细节的词
        - ❌ "估计是工具没调通"、"数据获取失败"等暴露后台问题的词
        - ❌ 任何提到"工具"、"系统"、"数据库"、"接口"等技术词汇的表达

        ✅ **必须遵守的风格**：
        - 语气轻松、活泼、亲切，就像和朋友聊天
        - 用"你"而不是"您"
        - 多用口语词："挺"、"特别"、"可多了"、"嘛"
        - 可以用"对了"、"话说"、"其实"等连接词
        - 适当用"～"、"！"，但不要堆砌符号
        - 不要用表格、不要用大量emoji
        - 就像本地朋友在给你推荐，不是客服在念稿子
        - **绝对不能暴露你获取信息的过程**：不要说"查了"、"查到"、"获取"等词
        - 要像你本来就知道这些信息一样自然地告诉用户
        - **当数据不完整或获取失败时**：不要说"工具没调通"、"系统说"，而是用"暂时没有相关信息"、"这方面的数据还在更新中"等自然表达
        - **无论工具调用成功还是失败，都要保持朋友聊天的语气，不能暴露任何技术细节**

        **5. 边界限制**
        - 只回答与陕西旅游、景点查询、行程规划相关的问题
        - 不回答与旅游无关的政治、经济、技术等问题
        - 不提供医疗、法律等专业建议

        **6. 回答示例（必须严格模仿这种风格，不要偏离）**

        ❌ **严重错误示例**（绝对不能这样回答）：
        "您好！刚刚通过官方工具实时查询，已获取**大唐芙蓉园最新、最权威的门票信息**：
        ✅ **门票价格：¥167.00/人**
        ✅ **位置**：西安市雁塔区..."

        "我刚刚通过 searchScenicSpot 工具查询到..."
        "根据工具返回的数据显示..."

        ✅ **正确示例**（轻松、自然、像朋友聊天）：

        用户问："大唐芙蓉园门票多少钱？"
        回答："大唐芙蓉园现在门票是167元一个人，包含了所有演出和展馆，性价比挺高的！园区每天9点到晚上10点都开放，在曲江那边，坐地铁4号线到曲江池西站下车走几分钟就到了。

        对了，建议你提前在【大唐芙蓉园】微信公众号上买票，现场不卖当日票的。如果你是学生的话可以半价，老人和小孩还有免票政策呢。晚上去最合适，能看到《梦回大唐》演出和水幕电影，灯光也特别漂亮！"

        用户问："咸阳有什么好玩的景点？"
        回答："咸阳好玩的地方挺多的！乾陵是武则天和唐高宗的合葬陵，门票103元，神道石像生特别震撼。汉阳陵也值得去，60元门票，地下博物馆的玻璃栈道直接踩在陪葬坑上，很有意思。

        还有郑国渠风景区，65元门票，山水挺清奇的。如果想体验民俗文化，马嵬驿和袁家村都是免费的，可以吃吃逛逛，特别接地气。你想玩历史文化类的还是民俗美食类的？"

        ❌ **错误示例**（绝对不能这样说）：
        "哇！这下可太全啦～刚帮你查了咸阳的'硬核宝藏清单'，连门票、特色、怎么玩都齐了..."
        "帮你查到了咸阳5个必去景点..."
        "查询到咸阳有以下景点..."

        用户问："华山门票多少钱？"
        回答："华山门票是160元/人，索道的话西峰往返280元，北峰往返150元。华山挺险的，建议穿舒服的运动鞋，带点水和吃的。如果想看日出，可以考虑住山上，不过山上住宿条件一般，价格也贵一些。你打算爬山还是坐索道上去？"

        用户问："陕西有什么景点？"
        回答："陕西好玩的地方可多了！西安有兵马俑、大雁塔、大唐芙蓉园这些必去的；咸阳有乾陵、汉阳陵，都是皇家陵墓，很震撼；延安有革命圣地，壶口瀑布也特别壮观；汉中那边山水挺美的，还有大熊猫基地。

        你想玩历史文化的还是自然风光的？或者具体想去哪个城市，我可以详细给你推荐～"

        ❌ **错误示例**（当工具未返回数据时，绝对不能这样说）：
        "哎？系统说'暂无景点推荐'……这可不对劲啊！估计是工具没调通..."
        "工具返回说该地区暂无景点推荐..."
        "刚查了但是没找到数据..."

        **核心原则**：
        - 用"你"而不是"您"（更亲切）
        - 用"挺"、"特别"、"可"等口语词
        - 可以用"对了"、"话说"、"其实"等连接词
        - 适当用"～"、"！"增加亲和力
        - 不要堆砌emoji和符号
        - 不要说"我查询到"、"实时获取"等技术词汇
        - 就像你本地的朋友在给你推荐一样自然

        🚨 **最后强调一次**：
        如果你的回答中出现"刚帮你查了"、"查到了"、"获取到"等词，那就是完全错误的！
        正确的表达应该是："咸阳好玩的地方挺多的！乾陵门票103元..."这种自然的表达！
        记住：你是朋友，不是客服！不是机器人！不要暴露任何查询过程和技术细节！
        要像你本来就很熟悉这些地方一样，直接告诉用户信息，而不是说"我帮你查了"！
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

            // 4. 智能判断是否需要强制工具调用
            String toolChoice = determineToolChoice(message);

            // 5. 判断是否为规划请求
            boolean isPlanningReq = isPlanningRequest(message);

            // 6. 调用流式API
            String assistantResponse = callDeepSeekStreamWithTools(messages, tools, emitter, sessionId, toolChoice, message, isPlanningReq);

            // 6. 保存新消息(只保存到数据库)
            saveMessages(sessionId, message, assistantResponse);

            // 7. 判断是否需要提取结构化数据
            if (isPlanningRequest(message)) {
                log.info("[ChatStreamService] 检测到行程规划请求,开始提取结构化数据");
                try {
                    com.navigation.vo.TravelPlanVO plan = travelPlanExtractService.extractPlan(assistantResponse);
                    if (plan != null) {
                        emitter.send(com.navigation.utils.StreamEventVOBuilder.buildPlanEvent(plan));
                        log.info("[ChatStreamService] 结构化数据发送成功 | 天数={}", plan.getDays().size());
                    }
                } catch (Exception e) {
                    log.error("[ChatStreamService] 提取结构化数据失败 | error={}", e.getMessage(), e);
                }
            }

            // 8. 发送关闭事件
            emitter.send(com.navigation.utils.StreamEventVOBuilder.buildCloseEvent());

            // 9. 完成SSE连接
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
     * 智能判断是否需要强制工具调用
     * 根据用户问题的关键词判断是否需要查询具体信息
     */
    private String determineToolChoice(String message) {
        String lowerMessage = message.toLowerCase();

        // 检测是否包含需要查询具体信息的关键词
        boolean needsToolCall =
            // 景点相关查询
            lowerMessage.contains("门票") || lowerMessage.contains("价格") || lowerMessage.contains("多少钱") ||
            lowerMessage.contains("开放时间") || lowerMessage.contains("几点开门") || lowerMessage.contains("营业时间") ||
            lowerMessage.contains("在哪") || lowerMessage.contains("位置") || lowerMessage.contains("地址") ||
            lowerMessage.contains("怎么去") || lowerMessage.contains("介绍") ||
            // 推荐类查询
            lowerMessage.contains("推荐") || lowerMessage.contains("有什么") || lowerMessage.contains("有哪些") ||
            lowerMessage.contains("好玩的") || lowerMessage.contains("景点") ||
            // 酒店查询
            lowerMessage.contains("酒店") || lowerMessage.contains("住宿") || lowerMessage.contains("宾馆") ||
            // 美食查询
            lowerMessage.contains("美食") || lowerMessage.contains("好吃的") || lowerMessage.contains("吃什么") ||
            lowerMessage.contains("餐厅") || lowerMessage.contains("小吃");

        if (needsToolCall) {
            log.info("[ChatStreamService] 检测到需要工具调用的问题 | message={} | toolChoice=required", message);
            return "required";  // 强制必须使用工具
        } else {
            log.info("[ChatStreamService] 普通对话问题 | message={} | toolChoice=auto", message);
            return "auto";  // 自动判断
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
    private String callDeepSeekStreamWithTools(JSONArray messages, JSONArray tools, SseEmitter emitter, String sessionId, String toolChoice, String userMessage, boolean isPlanningReq) throws IOException {
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
        requestBody.put("tool_choice", toolChoice);  // 动态设置：auto=自动判断, required=必须使用工具
        requestBody.put("stream", true);
        requestBody.put("temperature", 0.7);

        log.info("[ChatStreamService] 请求配置 | toolChoice={}", toolChoice);

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
        // 使用 Map 存储多个工具调用：key=toolCallId, value={id, name, args}
        Map<String, JSONObject> toolCallsMap = new java.util.HashMap<>();
        int messageIndex = 0;  // 消息序号

        // 增量提取相关变量
        // isPlanningReq 参数从外部传入,工具调用后递归时保持原值
        int lastExtractedLength = 0;  // 上次提取时的文本长度
        int extractedDayCount = 0;    // 已提取的天数

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

                        // 增量提取结构化数据(如果是规划请求)
                        if (isPlanningReq && fullResponse.length() - lastExtractedLength > 200) {
                            int newDayCount = tryIncrementalExtract(fullResponse.toString(), emitter, extractedDayCount);
                            if (newDayCount > extractedDayCount) {
                                extractedDayCount = newDayCount;  // 更新已提取天数
                                log.info("[ChatStreamService] 更新已提取天数 | extractedDayCount={}", extractedDayCount);
                            }
                            lastExtractedLength = fullResponse.length();
                        }
                    }

                    // 处理工具调用
                    JSONArray toolCalls = delta.getJSONArray("tool_calls");
                    if (toolCalls != null && !toolCalls.isEmpty()) {
                        for (int i = 0; i < toolCalls.size(); i++) {
                            JSONObject toolCall = toolCalls.getJSONObject(i);

                            // 获取 index（用于区分多个工具调用）
                            Integer index = toolCall.getInteger("index");
                            if (index == null) {
                                index = 0; // 默认为 0
                            }

                            String mapKey = String.valueOf(index);

                            // 获取或创建该 index 的工具调用对象
                            JSONObject toolCallObj = toolCallsMap.computeIfAbsent(mapKey, k -> {
                                JSONObject obj = new JSONObject();
                                obj.put("id", "");
                                obj.put("name", "");
                                obj.put("arguments", "");
                                return obj;
                            });

                            // 获取tool call id
                            String id = toolCall.getString("id");
                            if (id != null && !id.isEmpty()) {
                                toolCallObj.put("id", id);
                            }

                            // 获取function信息
                            JSONObject function = toolCall.getJSONObject("function");
                            if (function != null) {
                                String name = function.getString("name");
                                if (name != null && !name.isEmpty()) {
                                    toolCallObj.put("name", toolCallObj.getString("name") + name);
                                }

                                String arguments = function.getString("arguments");
                                if (arguments != null && !arguments.isEmpty()) {
                                    toolCallObj.put("arguments", toolCallObj.getString("arguments") + arguments);
                                }
                            }

                            toolCallsMap.put(mapKey, toolCallObj);
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
        if (!toolCallsMap.isEmpty()) {
            // 将 Map 转换为 List
            for (Map.Entry<String, JSONObject> entry : toolCallsMap.entrySet()) {
                JSONObject toolCallData = entry.getValue();
                String toolId = toolCallData.getString("id");
                String toolName = toolCallData.getString("name");
                String toolArgs = toolCallData.getString("arguments");

                if (toolId != null && !toolId.isEmpty() && toolName != null && !toolName.isEmpty()) {
                    JSONObject toolCallObj = new JSONObject();
                    toolCallObj.put("id", toolId);
                    toolCallObj.put("type", "function");
                    JSONObject funcObj = new JSONObject();
                    funcObj.put("name", toolName);
                    funcObj.put("arguments", toolArgs);
                    toolCallObj.put("function", funcObj);
                    toolCallsList.add(toolCallObj);

                    log.info("[ChatStreamService] 检测到工具调用 | tool={} | args={}",
                        toolName, toolArgs);
                }
            }

            if (!toolCallsList.isEmpty()) {
                log.info("[ChatStreamService] 共检测到 {} 个工具调用", toolCallsList.size());
                // 处理工具调用并继续
                return handleToolCallsAndContinue(messages, toolCallsList, emitter, sessionId);
            }
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

            // 完整打印工具返回结果
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

        // 4. 添加明确的指引消息,强调使用工具结果回答用户问题
        JSONObject guideMsg = new JSONObject();
        guideMsg.put("role", "system");
        guideMsg.put("content", "⚠️ 重要提示：你刚刚调用了工具并获得了查询结果。" +
            "现在请基于这些工具返回的真实数据，直接回答用户的问题。" +
            "不要说'工具没调通'、'暂无数据'等话，工具已经成功返回结果了！" +
            "请用轻松自然的语气，像朋友一样介绍这些信息给用户。");
        messages.add(guideMsg);

        // 5. 重新构建工具定义
        JSONArray tools = buildToolDefinitions();

        // 5. 递归调用API获取最终回复（工具执行后使用auto模式，保持isPlanningReq原值）
        // 从messages中找到最后一个user消息,判断是否为规划请求
        boolean isPlanningReq = false;
        for (int i = messages.size() - 1; i >= 0; i--) {
            JSONObject msg = messages.getJSONObject(i);
            if ("user".equals(msg.getString("role"))) {
                String userContent = msg.getString("content");
                isPlanningReq = isPlanningRequest(userContent);
                break;
            }
        }
        return callDeepSeekStreamWithTools(messages, tools, emitter, sessionId, "auto", "", isPlanningReq);
    }

    /**
     * 执行具体工具
     */
    private String executeToolCall(String toolName, String argumentsJson) {
        try {
            log.info("[ChatStreamService] 开始执行工具 | tool={} | args={} | aiTravelTools={} | aiTravelTools类型={}",
                toolName, argumentsJson,
                (aiTravelTools != null ? "已注入" : "NULL"),
                (aiTravelTools != null ? aiTravelTools.getClass().getName() : "NULL"));

            JSONObject args = JSON.parseObject(argumentsJson);

            switch (toolName) {
                case "searchScenicSpot":
                    String scenicName = args.getString("scenicName");
                    log.info("[ChatStreamService] 调用searchScenicSpot | scenicName={}", scenicName);
                    String result = aiTravelTools.searchScenicSpot(scenicName);
                    log.info("[ChatStreamService] searchScenicSpot返回 | result={}", result);
                    return result;

                case "recommendScenics":
                    String regionName = args.getString("regionName");
                    log.info("[ChatStreamService] 调用recommendScenics | regionName={}", regionName);
                    return aiTravelTools.recommendScenics(regionName);

                case "searchHotels":
                    String query = args.getString("query");
                    log.info("[ChatStreamService] 调用searchHotels | query={}", query);
                    return aiTravelTools.searchHotels(query);

                case "recommendFoods":
                    String region = args.getString("region");
                    log.info("[ChatStreamService] 调用recommendFoods | region={}", region);
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

    /**
     * 判断是否为行程规划请求
     */
    private boolean isPlanningRequest(String message) {
        String lowerMessage = message.toLowerCase();
        return lowerMessage.contains("规划") ||
               lowerMessage.contains("行程") ||
               lowerMessage.contains("几天") ||
               lowerMessage.contains("天游") ||
               lowerMessage.contains("怎么玩") ||
               lowerMessage.contains("路线") ||
               lowerMessage.contains("攻略") ||
               (lowerMessage.matches(".*\\d+天.*") && (lowerMessage.contains("西安") || lowerMessage.contains("陕西")));
    }

    /**
     * 尝试增量提取结构化数据
     * 每当文本增长到一定程度,就尝试提取新的一天的数据
     * @return 当前已提取的总天数
     */
    private int tryIncrementalExtract(String currentText, SseEmitter emitter, int extractedDayCount) {
        try {
            // 使用正则快速提取(不调用AI,保持性能)
            com.navigation.vo.TravelPlanVO plan = travelPlanExtractService.extractPlan(currentText);

            if (plan != null && plan.getDays() != null && plan.getDays().size() > extractedDayCount) {
                // 只发送新提取的天数
                List<com.navigation.vo.TravelPlanVO.DayPlan> newDays = plan.getDays().subList(
                    extractedDayCount,
                    plan.getDays().size()
                );

                // 构建增量数据
                com.navigation.vo.TravelPlanVO incrementalPlan = com.navigation.vo.TravelPlanVO.builder()
                        .days(newDays)
                        .build();

                emitter.send(com.navigation.utils.StreamEventVOBuilder.buildPlanEvent(incrementalPlan));

                log.info("[ChatStreamService] 增量提取成功 | 新增天数={} | 总天数={}",
                    newDays.size(), plan.getDays().size());

                return plan.getDays().size();  // 返回当前总天数
            }

            return extractedDayCount;  // 没有新数据,返回原值

        } catch (Exception e) {
            log.warn("[ChatStreamService] 增量提取失败 | error={}", e.getMessage());
            return extractedDayCount;  // 失败时返回原值
        }
    }
}
