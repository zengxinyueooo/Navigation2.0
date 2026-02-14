package com.navigation.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 聊天会话VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatSessionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 会话名称
     */
    private String sessionName;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 最后一条消息
     */
    private String lastMessage;

    /**
     * 消息数量
     */
    private Integer messageCount;
}
