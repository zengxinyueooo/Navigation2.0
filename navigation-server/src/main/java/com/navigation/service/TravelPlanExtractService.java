package com.navigation.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.navigation.config.QwenConfig;
import com.navigation.entity.Scenic;
import com.navigation.result.Result;
import com.navigation.vo.TravelPlanVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 旅游规划提取服务
 * 从AI生成的文本中提取结构化数据
 */
@Slf4j
@Service
public class TravelPlanExtractService {

    @Autowired
    private QwenConfig qwenConfig;

    @Autowired
    private ScenicService scenicService;

    /**
     * 从完整的旅游规划文本中提取结构化数据
     * @param planText AI生成的完整规划文本
     * @return 结构化的旅游规划数据
     */
    public TravelPlanVO extractPlan(String planText) {
        try {
            log.info("[TravelPlanExtractService] 开始提取结构化数据 | 文本长度={} | 文本前100字={}",
                planText.length(), planText.substring(0, Math.min(100, planText.length())));

            // 方案1: 使用正则表达式提取(快速但不够智能)
            TravelPlanVO plan = extractByRegex(planText);

            if (plan != null && plan.getDays() != null && !plan.getDays().isEmpty()) {
                log.info("[TravelPlanExtractService] 正则提取成功 | 天数={}", plan.getDays().size());
                // 打印每天的景点数量
                for (TravelPlanVO.DayPlan day : plan.getDays()) {
                    log.info("[TravelPlanExtractService] 第{}天 | 景点数={} | 预算={}",
                        day.getDay(),
                        day.getItems() != null ? day.getItems().size() : 0,
                        day.getDayCost());
                }
                return plan;
            }

            // 方案2: 如果正则提取失败,调用AI提取(智能但较慢)
            log.info("[TravelPlanExtractService] 正则提取失败,使用AI提取");
            plan = extractByAI(planText);

            if (plan != null && plan.getDays() != null) {
                log.info("[TravelPlanExtractService] AI提取完成 | 天数={}", plan.getDays().size());
            } else {
                log.warn("[TravelPlanExtractService] AI提取也失败了");
            }

            return plan;

        } catch (Exception e) {
            log.error("[TravelPlanExtractService] 提取失败 | error={}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 方案1: 使用正则表达式提取(适合格式规范的文本)
     */
    private TravelPlanVO extractByRegex(String planText) {
        try {
            List<TravelPlanVO.DayPlan> days = new ArrayList<>();

            // 匹配 "第X天" 或 "Day X"
            Pattern dayPattern = Pattern.compile("第(\\d+)天|Day\\s+(\\d+)", Pattern.CASE_INSENSITIVE);
            Matcher dayMatcher = dayPattern.matcher(planText);

            List<Integer> dayPositions = new ArrayList<>();
            List<Integer> dayNumbers = new ArrayList<>();

            while (dayMatcher.find()) {
                dayPositions.add(dayMatcher.start());
                String dayNum = dayMatcher.group(1) != null ? dayMatcher.group(1) : dayMatcher.group(2);
                dayNumbers.add(Integer.parseInt(dayNum));
            }

            // 提取每一天的内容
            for (int i = 0; i < dayPositions.size(); i++) {
                int start = dayPositions.get(i);
                int end = (i < dayPositions.size() - 1) ? dayPositions.get(i + 1) : planText.length();
                String dayText = planText.substring(start, end);

                TravelPlanVO.DayPlan dayPlan = extractDayPlan(dayNumbers.get(i), dayText);
                if (dayPlan != null && dayPlan.getItems() != null && !dayPlan.getItems().isEmpty()) {
                    days.add(dayPlan);
                    log.debug("[TravelPlanExtractService] 第{}天提取成功 | 景点数={}",
                        dayNumbers.get(i), dayPlan.getItems().size());
                }
            }

            if (days.isEmpty()) {
                return null;
            }

            // 计算总预算
            BigDecimal totalCost = days.stream()
                    .map(TravelPlanVO.DayPlan::getDayCost)
                    .filter(cost -> cost != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            return TravelPlanVO.builder()
                    .days(days)
                    .totalCost(totalCost)
                    .summary(generateSummary(days))
                    .build();

        } catch (Exception e) {
            log.error("[TravelPlanExtractService] 正则提取失败 | error={}", e.getMessage());
            return null;
        }
    }

    /**
     * 提取单天的行程
     */
    private TravelPlanVO.DayPlan extractDayPlan(int dayNumber, String dayText) {
        List<TravelPlanVO.PlanItem> items = new ArrayList<>();

        // 扩展景点名称匹配 - 匹配更多模式
        // 1. 匹配常见景点(带"景区"、"公园"、"博物馆"等后缀)
        Pattern scenicPattern1 = Pattern.compile("([\\u4e00-\\u9fa5]{2,10}(?:景区|公园|博物馆|遗址|陵|寺|塔|楼|宫|院|山|湖|池|园|城墙|古城))");

        // 2. 匹配硬编码的知名景点
        Pattern scenicPattern2 = Pattern.compile("(兵马俑|华清池|大雁塔|小雁塔|钟楼|鼓楼|城墙|回民街|乾陵|法门寺|华山|壶口瀑布|大唐芙蓉园|大唐不夜城|陕西历史博物馆|碑林|书院门|永兴坊|曲江池|大明宫|未央宫)");

        // 先匹配带后缀的景点
        Matcher scenicMatcher1 = scenicPattern1.matcher(dayText);
        while (scenicMatcher1.find()) {
            String scenicName = scenicMatcher1.group(1);
            // 避免重复添加
            boolean exists = items.stream().anyMatch(item -> item.getName().equals(scenicName));
            if (!exists) {
                items.add(TravelPlanVO.PlanItem.builder()
                        .type("scenic")
                        .name(scenicName)
                        .build());
            }
        }

        // 再匹配硬编码的知名景点
        Matcher scenicMatcher2 = scenicPattern2.matcher(dayText);
        while (scenicMatcher2.find()) {
            String scenicName = scenicMatcher2.group(1);
            // 避免重复添加
            boolean exists = items.stream().anyMatch(item -> item.getName().equals(scenicName));
            if (!exists) {
                items.add(TravelPlanVO.PlanItem.builder()
                        .type("scenic")
                        .name(scenicName)
                        .build());
            }
        }

        // 提取价格信息(匹配"XX元"、"¥XX"、"XX块"等模式)
        Pattern pricePattern = Pattern.compile("(?:¥|门票)?\\s*(\\d+)\\s*(?:元|块)");
        Matcher priceMatcher = pricePattern.matcher(dayText);
        BigDecimal dayCost = BigDecimal.ZERO;
        int priceIndex = 0;
        while (priceMatcher.find()) {
            BigDecimal price = new BigDecimal(priceMatcher.group(1));
            dayCost = dayCost.add(price);

            // 尝试将价格关联到对应的景点
            if (priceIndex < items.size()) {
                items.get(priceIndex).setPrice(price);
            }
            priceIndex++;
        }

        // 如果没有提取到任何景点,记录日志
        if (items.isEmpty()) {
            log.debug("[TravelPlanExtractService] 第{}天未提取到景点 | dayText前100字={}",
                dayNumber, dayText.substring(0, Math.min(100, dayText.length())));
        }

        return TravelPlanVO.DayPlan.builder()
                .day(dayNumber)
                .items(items)
                .dayCost(dayCost)
                .build();
    }

    /**
     * 方案2: 调用AI提取结构化数据(更智能但较慢)
     */
    private TravelPlanVO extractByAI(String planText) {
        try {
            CloseableHttpClient client = HttpClients.createDefault();
            HttpPost post = new HttpPost(qwenConfig.getApiUrl());

            post.setHeader("Content-Type", "application/json");
            post.setHeader("Authorization", "Bearer " + qwenConfig.getApiKey());

            // 构建提取prompt
            String extractPrompt = String.format("""
                请从以下旅游规划文本中提取结构化数据,返回JSON格式:

                要求的JSON格式:
                {
                  "days": [
                    {
                      "day": 1,
                      "items": [
                        {
                          "type": "scenic",
                          "name": "景点名",
                          "time": "09:00-12:00",
                          "price": 120,
                          "location": "从文本中提取的位置信息",
                          "description": "从文本中提取的景点描述(50字以内)"
                        }
                      ]
                    }
                  ],
                  "totalCost": 1500
                }

                重要提示:
                1. location和description必须从文本中提取,不要编造
                2. 如果文本中没有明确的location或description,则设为null
                3. description限制在50字以内
                4. 只提取文本中明确提到的信息

                旅游规划文本:
                %s

                请只返回JSON,不要有其他文字。
                """, planText);

            JSONObject requestBody = new JSONObject();
            requestBody.put("model", qwenConfig.getModelName());

            com.alibaba.fastjson.JSONArray messages = new com.alibaba.fastjson.JSONArray();
            JSONObject userMsg = new JSONObject();
            userMsg.put("role", "user");
            userMsg.put("content", extractPrompt);
            messages.add(userMsg);

            requestBody.put("messages", messages);
            requestBody.put("temperature", 0.1); // 低temperature确保输出稳定

            post.setEntity(new StringEntity(requestBody.toJSONString(), StandardCharsets.UTF_8));

            CloseableHttpResponse response = client.execute(post);
            String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);

            JSONObject jsonResponse = JSON.parseObject(responseBody);
            String content = jsonResponse.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content");

            // 提取JSON(去除可能的markdown代码块标记)
            content = content.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();

            TravelPlanVO plan = JSON.parseObject(content, TravelPlanVO.class);

            // 后处理逻辑(快速执行,避免阻塞SSE)
            if (plan != null && plan.getDays() != null) {
                // 1. 计算每天的dayCost(如果AI没有返回)
                for (TravelPlanVO.DayPlan day : plan.getDays()) {
                    if (day.getDayCost() == null && day.getItems() != null) {
                        BigDecimal cost = day.getItems().stream()
                            .map(TravelPlanVO.PlanItem::getPrice)
                            .filter(p -> p != null)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                        day.setDayCost(cost);
                    }
                }

                // 2. 生成summary(如果AI没有返回)
                if (plan.getSummary() == null || plan.getSummary().isEmpty()) {
                    plan.setSummary(generateSummary(plan.getDays()));
                }

                // 3. 重新计算totalCost(确保准确性)
                BigDecimal totalCost = plan.getDays().stream()
                    .map(TravelPlanVO.DayPlan::getDayCost)
                    .filter(cost -> cost != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                plan.setTotalCost(totalCost);
            }

            response.close();
            client.close();

            return plan;

        } catch (Exception e) {
            log.error("[TravelPlanExtractService] AI提取失败 | error={}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 从Redis查询并填充景点真实信息(快速查询)
     */
    private void enrichItemFromRedis(TravelPlanVO.PlanItem item) {
        try {
            // 从Redis查询景点信息
            Result<List<Scenic>> result = scenicService.queryScenicByName(item.getName());

            if (result != null && result.getData() != null && !result.getData().isEmpty()) {
                Scenic scenic = result.getData().get(0);

                // 填充真实的location和description
                if (scenic.getScenicLocateDescription() != null) {
                    item.setLocation(scenic.getScenicLocateDescription());
                }

                if (scenic.getScenicDescription() != null) {
                    // description可能很长,截取前100字
                    String desc = scenic.getScenicDescription();
                    if (desc.length() > 100) {
                        desc = desc.substring(0, 100) + "...";
                    }
                    item.setDescription(desc);
                }
            }
        } catch (Exception e) {
            // 静默失败,不影响主流程
            log.debug("[TravelPlanExtractService] Redis查询失败 | name={}", item.getName());
        }
    }

    /**
     * 生成摘要
     */
    private String generateSummary(List<TravelPlanVO.DayPlan> days) {
        if (days == null || days.isEmpty()) {
            return "";
        }

        StringBuilder summary = new StringBuilder();
        summary.append(days.size()).append("天行程,包含");

        long scenicCount = days.stream()
                .flatMap(day -> day.getItems().stream())
                .filter(item -> "scenic".equals(item.getType()))
                .count();

        if (scenicCount > 0) {
            summary.append(scenicCount).append("个景点");
        }

        return summary.toString();
    }
}
