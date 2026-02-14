package com.navigation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI聊天消息实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("chat_message")
public class ChatMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 消息类型(user-用户,assistant-AI,tool-工具调用)
     */
    private String messageType;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 工具名称(仅tool类型)
     */
    private String toolName;

    /**
     * 工具执行结果(仅tool类型)
     */
    private String toolResult;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
