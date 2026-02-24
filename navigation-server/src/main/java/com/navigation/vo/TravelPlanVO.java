package com.navigation.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 旅游规划结构化数据VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TravelPlanVO {

    /**
     * 行程天数
     */
    private List<DayPlan> days;

    /**
     * 总预算
     */
    private BigDecimal totalCost;

    /**
     * 行程摘要
     */
    private String summary;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DayPlan {
        /**
         * 第几天
         */
        private Integer day;

        /**
         * 日期描述
         */
        private String date;

        /**
         * 当天行程项
         */
        private List<PlanItem> items;

        /**
         * 当天预算
         */
        private BigDecimal dayCost;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlanItem {
        /**
         * 类型: scenic(景点), hotel(酒店), food(美食), transport(交通)
         */
        private String type;

        /**
         * 数据库ID
         */
        private Long id;

        /**
         * 名称
         */
        private String name;

        /**
         * 时间段
         */
        private String time;

        /**
         * 价格
         */
        private BigDecimal price;

        /**
         * 位置
         */
        private String location;

        /**
         * 描述
         */
        private String description;
    }
}
