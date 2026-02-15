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

        // 第二步：根据景点ID获取票务信息
        List<Ticket> tickets = (List<Ticket>) ticketService.queryByScenicName(scenic.getScenicName());

        if (tickets.isEmpty()) {
            return String.format("景点：%s 没有门票信息。", scenic.getScenicName());
        }

        // 假设我们只取第一个票种的价格
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

        // 调用查询方法，获取景点列表
        Result<List<Scenic>> result = scenicService.queryPageByRegionName(1, 10, regionName);

        // 如果没有找到景点，返回提示信息
        if (result == null || result.getData() == null || result.getData().isEmpty()) {
            return "该地区暂无景点推荐。";
        }

        // 将景点信息按格式拼接成字符串并返回
        return result.getData().stream()
                .map(s -> String.format("🏛 %s - %s", s.getScenicName(), s.getScenicLocateDescription()))
                .collect(Collectors.joining("\n"));
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
