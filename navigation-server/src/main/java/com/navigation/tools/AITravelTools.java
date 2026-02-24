package com.navigation.tools;

import com.navigation.entity.Food;
import com.navigation.entity.Hotel;
import com.navigation.entity.Scenic;
import com.navigation.entity.Ticket;
import com.navigation.result.Result;


import com.navigation.service.FoodService;
import com.navigation.service.HotelService;
import com.navigation.service.ScenicService;
import com.navigation.service.TicketService;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.P;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service("aiTravelTools")  // Bean 名称要和 AiService 注解的 tools 对应
public class AITravelTools {

    @Autowired
    private ScenicService scenicService;

    @Autowired
    private HotelService hotelService;

    @Autowired
    private FoodService foodService;


    @Autowired
    private TicketService ticketService;


    // ================= 工具 1：查询景点信息 =================
    @Tool("查询景点详细信息,包括介绍、位置、开放时间、门票价格等")
    public String searchScenicSpot(@P("景点名称") String scenicName) {
        log.info("[AITravelTools] searchScenicSpot被调用 | scenicName={} | scenicService={} | scenicService类型={}",
            scenicName,
            (scenicService != null ? "已注入" : "NULL"),
            (scenicService != null ? scenicService.getClass().getName() : "NULL"));

        Result<List<Scenic>> result = scenicService.queryScenicByName(scenicName);

        log.info("[AITravelTools] queryScenicByName返回 | result={}", (result != null ? "非空" : "NULL"));

        if (result == null || result.getData() == null || result.getData().isEmpty()) {
            return "未找到景点：" + scenicName + "，请提供完整的景点名称。";
        }

        Scenic scenic = result.getData().get(0); // 取第一个匹配的景点

        // 第二步：根据景点名称获取票务信息
        Result<List<Ticket>> ticketResult = ticketService.queryByScenicName(scenic.getScenicName());

        if (ticketResult == null || ticketResult.getData() == null || ticketResult.getData().isEmpty()) {
            return String.format("景点：%s 没有门票信息。", scenic.getScenicName());
        }

        // 假设我们只取第一个票种的价格
        List<Ticket> tickets = ticketResult.getData();
        Ticket ticket = tickets.get(0);

        return String.format("""
            景点名称：%s
            位置：%s
            介绍：%s
            开放时间：%s - %s
            门票价格：%s元
            状态：%s
            """,
                scenic.getScenicName(),
                scenic.getScenicLocateDescription(),
                scenic.getScenicDescription(),
                scenic.getOpenStartTime(),
                scenic.getOpenEndTime(),
                ticket.getPrice(),
                scenic.getScenicStatus()
        );
    }

    // ================= 工具 2：推荐景点 =================
    @Tool("根据地区推荐合适的景点")
    public String recommendScenics(@P("地区名称") String regionName) {
        // 添加调试日志
        log.info("[AITravelTools] recommendScenics被调用 | regionName={} | scenicService={}",
            regionName, (scenicService != null ? scenicService.getClass().getName() : "NULL"));

        // 特殊处理：如果用户问"陕西"，则查询所有陕西分区的景点
        List<String> shaanxiRegions = Arrays.asList("西安", "咸阳", "宝鸡", "渭南", "延安", "榆林", "汉中", "安康", "商洛", "铜川");

        List<Scenic> allScenics = new ArrayList<>();

        if ("陕西".equals(regionName) || "陕西省".equals(regionName)) {
            log.info("[AITravelTools] 检测到陕西省级查询，将查询所有分区景点");
            // 查询所有陕西分区的景点
            for (String region : shaanxiRegions) {
                Result<List<Scenic>> result = scenicService.queryPageByRegionName(1, 100, region);
                if (result != null && result.getData() != null && !result.getData().isEmpty()) {
                    allScenics.addAll(result.getData());
                }
            }

            // 如果找到了景点，取前20个返回
            if (!allScenics.isEmpty()) {
                return allScenics.stream()
                        .limit(20)
                        .map(s -> String.format("🏛 %s - %s（%s）",
                            s.getScenicName(),
                            s.getScenicLocateDescription(),
                            getRegionNameById(s.getRegionId())))
                        .collect(Collectors.joining("\n"));
            } else {
                return "陕西景点数据暂时无法获取，请稍后再试。";
            }
        } else {
            // 正常查询单个地区
            Result<List<Scenic>> result = scenicService.queryPageByRegionName(1, 10, regionName);

            // 如果没有找到景点，返回提示信息
            if (result == null || result.getData() == null || result.getData().isEmpty()) {
                return regionName + "的景点数据暂时无法获取，可以试试具体的地区名称，比如西安、咸阳等。";
            }

            // 将景点信息按格式拼接成字符串并返回
            return result.getData().stream()
                    .map(s -> String.format("🏛 %s - %s", s.getScenicName(), s.getScenicLocateDescription()))
                    .collect(Collectors.joining("\n"));
        }
    }

    // 辅助方法：根据regionId获取地区名称
    private String getRegionNameById(Integer regionId) {
        // 简单映射，实际可以从数据库或缓存获取
        switch (regionId) {
            case 1: return "西安";
            case 2: return "咸阳";
            case 3: return "宝鸡";
            case 4: return "渭南";
            case 5: return "延安";
            case 6: return "榆林";
            case 7: return "汉中";
            case 8: return "安康";
            case 9: return "商洛";
            case 10: return "铜川";
            default: return "未知地区";
        }
    }


    // ================= 工具 3：查询酒店 =================
    @Tool("查询酒店信息,可以输入酒店名或地区")
    public String searchHotels(@P("酒店名称或地区") String query) {
        Result<List<Hotel>> result = hotelService.searchHotels(query);

        if (result == null || result.getData() == null || result.getData().isEmpty()) {
            return "未找到符合条件的酒店。";
        }

        return result.getData().stream()
                .map(h -> String.format("🏨 %s - %s (评分:%.1f)",
                        h.getHotelName(), h.getAddress(), h.getAverageMark()))
                .collect(Collectors.joining("\n"));
    }

    // ================= 工具 4：推荐美食 =================
    @Tool("推荐当地特色美食")
    public String recommendFoods(@P("地区名称") String region) {
        Result<List<Food>> result = foodService.findByRegion(region);

        if (result == null || result.getData() == null || result.getData().isEmpty()) {
            return "未找到该地区的美食推荐。";
        }

        return result.getData().stream()
                .map(f -> String.format("🍜 %s - %s ",
                        f.getFoodName(), f.getFoodDescription()))
                .collect(Collectors.joining("\n"));
    }


}
