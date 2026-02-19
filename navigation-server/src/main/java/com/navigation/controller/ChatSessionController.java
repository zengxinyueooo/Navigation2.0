package com.navigation.controller;

import com.navigation.context.BaseContext;
import com.navigation.result.Result;
import com.navigation.service.ChatSessionService;
import com.navigation.vo.ChatMessageVO;
import com.navigation.vo.ChatSessionVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 聊天会话管理Controller
 */
@Slf4j
@RestController
@RequestMapping("/AI/session")
public class ChatSessionController {

    @Autowired
    private ChatSessionService chatSessionService;

    /**
     * 创建新会话（必须登录，自动从token获取userId）
     * @return 会话ID
     */
    @PostMapping("/create")
    public Result<String> createSession() {
        try {
            Integer userId = BaseContext.getUserId();
            String sessionId = chatSessionService.createSession(userId != null ? userId.longValue() : null);
            log.info("[ChatSessionController] 创建新会话 | sessionId={} | userId={}", sessionId, userId);
            return Result.success(sessionId);
        } catch (RuntimeException e) {
            log.error("[ChatSessionController] 创建会话失败 | error={}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取用户的所有会话列表（必须登录，自动从token获取userId）
     * @return 会话列表
     */
    @GetMapping("/list")
    public Result<List<ChatSessionVO>> listSessions() {
        try {
            Integer userId = BaseContext.getUserId();
            List<ChatSessionVO> sessions = chatSessionService.listSessions(userId != null ? userId.longValue() : null);
            return Result.success(sessions);
        } catch (RuntimeException e) {
            log.error("[ChatSessionController] 获取会话列表失败 | error={}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除会话（必须登录，自动从token获取userId，只能删除自己的会话）
     * @param sessionId 会话ID
     * @return 成功/失败
     */
    @DeleteMapping("/{sessionId}")
    public Result<Void> deleteSession(@PathVariable("sessionId") String sessionId) {
        try {
            Integer userId = BaseContext.getUserId();
            chatSessionService.deleteSession(userId != null ? userId.longValue() : null, sessionId);
            log.info("[ChatSessionController] 删除会话成功 | sessionId={} | userId={}", sessionId, userId);
            return Result.success();
        } catch (RuntimeException e) {
            log.error("[ChatSessionController] 删除会话失败 | sessionId={} | error={}",
                sessionId, e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取会话的历史消息（必须登录，只能查看自己的会话）
     * @param sessionId 会话ID
     * @return 消息列表
     */
    @GetMapping("/{sessionId}/messages")
    public Result<List<ChatMessageVO>> getSessionMessages(@PathVariable("sessionId") String sessionId) {
        try {
            Integer userId = BaseContext.getUserId();

            // 验证会话所有权
            if (!chatSessionService.validateSession(userId != null ? userId.longValue() : null, sessionId)) {
                log.warn("[ChatSessionController] 权限验证失败 | sessionId={} | userId={}", sessionId, userId);
                return Result.error("无权访问该会话");
            }

            List<ChatMessageVO> messages = chatSessionService.getSessionMessages(sessionId);
            return Result.success(messages);
        } catch (RuntimeException e) {
            log.error("[ChatSessionController] 获取会话消息失败 | sessionId={} | error={}",
                sessionId, e.getMessage());
            return Result.error(e.getMessage());
        }
    }
}
