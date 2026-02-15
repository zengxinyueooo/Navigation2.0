package com.navigation.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StreamEventDataVO {
    /**
     * 事件类型: mainText, error等
     */
    private String event;

    /**
     * 内容(累积的完整文本)
     */
    private String content;
}
