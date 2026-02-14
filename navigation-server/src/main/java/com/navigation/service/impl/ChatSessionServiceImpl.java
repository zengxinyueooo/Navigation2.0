package com.navigation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.navigation.entity.ChatMessage;
import com.navigation.entity.ChatSession;
import com.navigation.mapper.ChatMessageMapper;
import com.navigation.mapper.ChatSessionMapper;
import com.navigation.service.ChatSessionService;
import com.navigation.vo.ChatMessageVO;
import com.navigation.vo.ChatSessionVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 聊天会话服务实现类
 */
@Slf4j
@Service
public class ChatSessionServiceImpl implements ChatSessionService {

    @Autowired
    private ChatSessionMapper chatSessionMapper;

    @Autowired
    private ChatMessageMapper chatMessageMapper;

    @Override
    @Transactional
    public String createSession(Long userId) {
        String sessionId = UUID.randomUUID().toString().replace("-", "");

        ChatSession session = new ChatSession();
        session.setSessionId(sessionId);
        session.setUserId(userId);
        session.setSessionName("新对话");
        session.setCreateTime(LocalDateTime.now());
        session.setUpdateTime(LocalDateTime.now());
        session.setIsDeleted(0);

        chatSessionMapper.insert(session);

        log.info("[ChatSessionService] 创建新会话 | sessionId={} | userId={}", sessionId, userId);
        return sessionId;
    }

    @Override
    public List<ChatSessionVO> listSessions(Long userId) {
        QueryWrapper<ChatSession> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
               .eq("is_deleted", 0)
               .orderByDesc("update_time");

        List<ChatSession> sessions = chatSessionMapper.selectList(wrapper);

        return sessions.stream().map(session -> {
            ChatSessionVO vo = new ChatSessionVO();
            vo.setSessionId(session.getSessionId());
            vo.setSessionName(session.getSessionName());
            vo.setCreateTime(session.getCreateTime());
            vo.setUpdateTime(session.getUpdateTime());

            // 获取最后一条消息
            QueryWrapper<ChatMessage> msgWrapper = new QueryWrapper<>();
            msgWrapper.eq("session_id", session.getSessionId())
                     .orderByDesc("create_time")
                     .last("LIMIT 1");
            ChatMessage lastMsg = chatMessageMapper.selectOne(msgWrapper);
            if (lastMsg != null) {
                vo.setLastMessage(lastMsg.getContent());
            }

            // 获取消息数量
            QueryWrapper<ChatMessage> countWrapper = new QueryWrapper<>();
            countWrapper.eq("session_id", session.getSessionId());
            Long count = Long.valueOf(chatMessageMapper.selectCount(countWrapper));
            vo.setMessageCount(count.intValue());

            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteSession(Long userId, String sessionId) {
        // 验证权限
        if (!validateSession(userId, sessionId)) {
            throw new RuntimeException("无权删除该会话");
        }

        // 软删除会话
        QueryWrapper<ChatSession> wrapper = new QueryWrapper<>();
        wrapper.eq("session_id", sessionId);
        ChatSession session = chatSessionMapper.selectOne(wrapper);
        if (session != null) {
            session.setIsDeleted(1);
            chatSessionMapper.updateById(session);
        }

        log.info("[ChatSessionService] 删除会话 | sessionId={} | userId={}", sessionId, userId);
    }

    @Override
    public List<ChatMessageVO> getSessionMessages(String sessionId) {
        QueryWrapper<ChatMessage> wrapper = new QueryWrapper<>();
        wrapper.eq("session_id", sessionId)
               .in("message_type", "user", "assistant")
               .orderByAsc("create_time");

        List<ChatMessage> messages = chatMessageMapper.selectList(wrapper);

        return messages.stream().map(msg -> {
            ChatMessageVO vo = new ChatMessageVO();
            vo.setMessageType(msg.getMessageType());
            vo.setContent(msg.getContent());
            vo.setCreateTime(msg.getCreateTime());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void saveUserMessage(String sessionId, String content) {
        ChatMessage message = new ChatMessage();
        message.setSessionId(sessionId);
        message.setMessageType("user");
        message.setContent(content);
        message.setCreateTime(LocalDateTime.now());

        chatMessageMapper.insert(message);

        // 更新会话的更新时间
        updateSessionTime(sessionId);

        log.debug("[ChatSessionService] 保存用户消息 | sessionId={}", sessionId);
    }

    @Override
    @Transactional
    public void saveAssistantMessage(String sessionId, String content) {
        ChatMessage message = new ChatMessage();
        message.setSessionId(sessionId);
        message.setMessageType("assistant");
        message.setContent(content);
        message.setCreateTime(LocalDateTime.now());

        chatMessageMapper.insert(message);

        // 更新会话的更新时间
        updateSessionTime(sessionId);

        log.debug("[ChatSessionService] 保存AI消息 | sessionId={}", sessionId);
    }

    @Override
    @Transactional
    public void saveToolCall(String sessionId, String toolName, String toolResult) {
        ChatMessage message = new ChatMessage();
        message.setSessionId(sessionId);
        message.setMessageType("tool");
        message.setContent("调用工具: " + toolName);
        message.setToolName(toolName);
        message.setToolResult(toolResult);
        message.setCreateTime(LocalDateTime.now());

        chatMessageMapper.insert(message);

        log.debug("[ChatSessionService] 保存工具调用 | sessionId={} | tool={}", sessionId, toolName);
    }

    @Override
    public boolean validateSession(Long userId, String sessionId) {
        QueryWrapper<ChatSession> wrapper = new QueryWrapper<>();
        wrapper.eq("session_id", sessionId)
               .eq("is_deleted", 0);

        ChatSession session = chatSessionMapper.selectOne(wrapper);

        if (session == null) {
            return false;
        }

        // 如果userId为null(游客),只检查会话存在
        if (userId == null) {
            return true;
        }

        // 验证会话是否属于该用户
        return session.getUserId() != null && session.getUserId().equals(userId);
    }

    @Override
    @Transactional
    public void updateSessionName(String sessionId, String firstMessage) {
        QueryWrapper<ChatSession> wrapper = new QueryWrapper<>();
        wrapper.eq("session_id", sessionId);

        ChatSession session = chatSessionMapper.selectOne(wrapper);
        if (session != null && "新对话".equals(session.getSessionName())) {
            // 截取前20个字符作为会话名称
            String name = firstMessage.length() > 20
                ? firstMessage.substring(0, 20) + "..."
                : firstMessage;
            session.setSessionName(name);
            chatSessionMapper.updateById(session);

            log.info("[ChatSessionService] 更新会话名称 | sessionId={} | name={}", sessionId, name);
        }
    }

    @Override
    public List<ChatMessageVO> getRecentMessages(String sessionId, int limit) {
        QueryWrapper<ChatMessage> wrapper = new QueryWrapper<>();
        wrapper.eq("session_id", sessionId)
               .in("message_type", "user", "assistant")  // 只获取对话消息,不含tool
               .orderByDesc("create_time")  // 降序获取最新的
               .last("LIMIT " + limit);

        List<ChatMessage> messages = chatMessageMapper.selectList(wrapper);

        // 反转为升序(时间从旧到新)
        Collections.reverse(messages);

        return messages.stream().map(msg -> {
            ChatMessageVO vo = new ChatMessageVO();
            vo.setMessageType(msg.getMessageType());
            vo.setContent(msg.getContent());
            vo.setCreateTime(msg.getCreateTime());
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 更新会话的更新时间
     */
    private void updateSessionTime(String sessionId) {
        QueryWrapper<ChatSession> wrapper = new QueryWrapper<>();
        wrapper.eq("session_id", sessionId);

        ChatSession session = chatSessionMapper.selectOne(wrapper);
        if (session != null) {
            session.setUpdateTime(LocalDateTime.now());
            chatSessionMapper.updateById(session);
        }
    }
}
