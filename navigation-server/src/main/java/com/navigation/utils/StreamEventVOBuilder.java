package com.navigation.utils;

import com.navigation.vo.StreamEventDataVO;
import com.navigation.vo.StreamEventVO;

public class StreamEventVOBuilder {

    /**
     * 构建开启事件
     */
    public static StreamEventVO buildOpenEvent() {
        return StreamEventVO.builder()
                .type("open")
                .build();
    }

    /**
     * 构建消息事件
     */
    public static StreamEventVO buildMessageEvent(int index, String content) {
        return StreamEventVO.builder()
                .type("message")
                .index(index)
                .data(StreamEventDataVO.builder()
                        .event("mainText")
                        .content(content)
                        .build())
                .build();
    }

    /**
     * 构建关闭事件
     */
    public static StreamEventVO buildCloseEvent() {
        return StreamEventVO.builder()
                .type("close")
                .build();
    }

    /**
     * 构建错误事件
     */
    public static StreamEventVO buildErrorEvent(String errorMessage) {
        return StreamEventVO.builder()
                .type("error")
                .data(StreamEventDataVO.builder()
                        .event("error")
                        .content(errorMessage)
                        .build())
                .build();
    }
}
