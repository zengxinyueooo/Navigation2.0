package com.navigation.controller;

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
     * 创建新会话
     * @param userId 用户ID(可选,不传表示游客)
     * @return 会话ID
     */
    @PostMapping("/create")
    public Result<String> createSession(@RequestParam(value = "userId", required = false) Long userId) {
        String sessionId = chatSessionService.createSession(userId);
        log.info("[ChatSessionController] 创建新会话 | sessionId={} | userId={}", sessionId, userId);
        return Result.success(sessionId);
    }

    /**
     * 获取用户的所有会话列表
     * @param userId 用户ID
     * @return 会话列表
     */
    @GetMapping("/list")
    public Result<List<ChatSessionVO>> listSessions(@RequestParam("userId") Long userId) {
        List<ChatSessionVO> sessions = chatSessionService.listSessions(userId);
        return Result.success(sessions);
    }

    /**
     * 删除会话
     * @param userId 用户ID
     * @param sessionId 会话ID
     * @return 成功/失败
     */
    @DeleteMapping("/{sessionId}")
    public Result<Void> deleteSession(
        @RequestParam("userId") Long userId,
        @PathVariable("sessionId") String sessionId
    ) {
        chatSessionService.deleteSession(userId, sessionId);
        log.info("[ChatSessionController] 删除会话 | sessionId={} | userId={}", sessionId, userId);
        return Result.success();
    }

    /**
     * 获取会话的历史消息
     * @param sessionId 会话ID
     * @return 消息列表
     */
    @GetMapping("/{sessionId}/messages")
    public Result<List<ChatMessageVO>> getSessionMessages(@PathVariable("sessionId") String sessionId) {
        List<ChatMessageVO> messages = chatSessionService.getSessionMessages(sessionId);
        return Result.success(messages);
    }
}
