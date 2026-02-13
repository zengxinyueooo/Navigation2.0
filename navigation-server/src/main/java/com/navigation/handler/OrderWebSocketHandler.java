package com.navigation.handler;

import io.swagger.models.auth.In;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
/**
 * 管理所有活跃的连接，建立用户与连接的映射关系
 */
@Slf4j
@Component
public class OrderWebSocketHandler extends TextWebSocketHandler {

    // 核心数据结构：用户ID -> WebSocket会话
    //userSessions映射表：这是我们的'通讯录'，记录每个用户对应的连接
    private static final Map<Long, WebSocketSession> userSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 连接建立时执行
        Long userId = (Long) session.getAttributes().get("userId");
        userSessions.compute(userId, (key, oldSession) -> {
            // 如果旧连接存在且没关闭，先关掉
            if (oldSession != null && oldSession.isOpen()) {
                try {
                    oldSession.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            // 返回新的 session，覆盖旧的
            return session;
        });
        // 记住这个连接,用户连接时，把他的ID和连接信息存到映射表
        log.info("用户 {} 连接成功", userId);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        // 连接关闭时执行
        Long userId = (Long) session.getAttributes().get("userId");
        userSessions.computeIfPresent(userId, (key, oldSession) -> {// 清理这个连接,用户断开时，从映射表中移除记录
            // 只在旧的 session 和当前关闭的一致时才移除
            if (oldSession == session) {
                return null; // 返回 null 表示从 Map 删除
            }
            return oldSession; // 如果不是同一个连接，不删除（可能用户已重新建立了新连接）
        });
        log.info("用户 {} 连接关闭", userId);
    }

    // 心跳检测机制，定期向客户端发送 "ping" 消息，确保连接保持活跃
    private void startHeartbeat(WebSocketSession session) {
        // 定期发送 "ping" 消息给客户端
        new Thread(() -> { //启动了一个新的线程，这样不会阻塞主线程
            while (session.isOpen()) { //判断当前 WebSocket 连接是否仍然处于打开状态。如果是，则继续循环；如果 WebSocket 连接被关闭，则停止循环
                try {
                    // 每30秒发送一次心跳消息,服务器会检查 WebSocket 连接的状态
                    Thread.sleep(30000);
                    if (session.isOpen()) {
                        session.sendMessage(new TextMessage("ping"));
                    }
                } catch (InterruptedException | IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void sendMessageToUser(Integer userId, String message) {
        WebSocketSession session = userSessions.get(userId);  // 获取用户的 WebSocket 会话
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(message));  // 向该用户发送消息
            } catch (IOException e) {
                log.error("消息推送失败，用户 {}，错误：{}", userId, e.getMessage());

                // 失败处理策略：
                // 1. 可以选择移除连接，下次让用户重新连接
                userSessions.remove(userId, session);

                // 2. 也可以把消息存储到消息队列/数据库，下次用户上线再补发
                // saveUnsentMessage(userId, message);
            }
        } else {
            log.warn("用户 {} 不在线，消息未推送", userId);
            // 同样可以考虑存储离线消息
            // saveUnsentMessage(userId, message);
        }
    }


    // 向所有连接的客户端推送消息
    public static void broadcastMessage(String message) {
        userSessions.forEach((sessionId, session) -> {
            try {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(message));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        if ("ping".equals(message.getPayload())) {
            // 响应服务器的ping消息
            session.sendMessage(new TextMessage("pong"));
        }
    }

}