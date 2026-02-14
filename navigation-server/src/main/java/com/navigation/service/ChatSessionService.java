package com.navigation.service;

import com.navigation.entity.ChatMessage;
import com.navigation.entity.ChatSession;
import com.navigation.vo.ChatMessageVO;
import com.navigation.vo.ChatSessionVO;

import java.util.List;

/**
 * 聊天会话服务接口
 */
public interface ChatSessionService {

    /**
     * 创建新会话
     * @param userId 用户ID(可为null,表示游客)
     * @return 会话ID
     */
    String createSession(Long userId);

    /**
     * 获取用户的所有会话列表
     * @param userId 用户ID(null表示游客,使用临时标识)
     * @return 会话列表
     */
    List<ChatSessionVO> listSessions(Long userId);

    /**
     * 删除会话
     * @param userId 用户ID
     * @param sessionId 会话ID
     */
    void deleteSession(Long userId, String sessionId);

    /**
     * 获取会话的历史消息
     * @param sessionId 会话ID
     * @return 消息列表
     */
    List<ChatMessageVO> getSessionMessages(String sessionId);

    /**
     * 保存用户消息
     * @param sessionId 会话ID
     * @param content 消息内容
     */
    void saveUserMessage(String sessionId, String content);

    /**
     * 保存AI消息
     * @param sessionId 会话ID
     * @param content 消息内容
     */
    void saveAssistantMessage(String sessionId, String content);

    /**
     * 保存工具调用记录
     * @param sessionId 会话ID
     * @param toolName 工具名称
     * @param toolResult 工具结果
     */
    void saveToolCall(String sessionId, String toolName, String toolResult);

    /**
     * 验证会话是否属于该用户
     * @param userId 用户ID
     * @param sessionId 会话ID
     * @return 是否有权访问
     */
    boolean validateSession(Long userId, String sessionId);

    /**
     * 更新会话名称(根据首条消息自动生成)
     * @param sessionId 会话ID
     * @param firstMessage 首条消息
     */
    void updateSessionName(String sessionId, String firstMessage);

    /**
     * 获取会话的最近N条消息(用于构建对话上下文)
     * @param sessionId 会话ID
     * @param limit 消息数量限制(建议20条)
     * @return 消息列表(按时间升序,只包含user和assistant类型)
     */
    List<ChatMessageVO> getRecentMessages(String sessionId, int limit);
}
