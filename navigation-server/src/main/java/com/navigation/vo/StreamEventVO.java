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
     * 事件类型: open, message, close, error
     */
    private String type;

    /**
     * 消息序号(仅message类型有)
     */
    private Integer index;

    /**
     * 数据体
     */
    private StreamEventDataVO data;
}
