package com.navigation.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StreamEventVO {
    /**
     * 事件类型: open, message, close, error, plan(结构化行程数据)
     */
    private String type;

    /**
     * 消息序号(仅message类型有)
     */
    private Integer index;

    /**
     * 数据体(文本消息)
     */
    private StreamEventDataVO data;

    /**
     * 结构化行程数据(仅plan类型有)
     */
    private TravelPlanVO plan;
}
