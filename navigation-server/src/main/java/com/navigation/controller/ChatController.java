package com.navigation.controller;

import com.navigation.aiservice.ConsultantService;
import com.navigation.service.ChatStreamService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/AI")
public class ChatController {
    @Autowired
    private ConsultantService consultantService;

    @Autowired
    private ChatStreamService chatStreamService;

    // 非流式版本 - 避免LangChain4J 0.30.0的Flux bug
    @GetMapping(value = "/chat", produces = "application/json;charset=utf-8")
    public String chat(@RequestParam("memoryId") String memoryId, @RequestParam("message") String message){
        return consultantService.chat(memoryId, message);
    }

    // 流式版本 - 使用自定义实现绕过LangChain4J的Flux bug
    @GetMapping(value = "/chatStream", produces = "text/event-stream;charset=utf-8")
    public SseEmitter chatStream(
        @RequestParam("memoryId") String memoryId,
        @RequestParam("message") String message
    ) {
        SseEmitter emitter = new SseEmitter(60000L); // 60秒超时

        // 异步处理
        CompletableFuture.runAsync(() -> {
            try {
                chatStreamService.streamChat(memoryId, message, emitter);
            } catch (Exception e) {
                log.error("[ChatController] 流式聊天失败 | memoryId={} | message={} | error={}",
                    memoryId, message, e.getMessage(), e);
                try {
                    emitter.send(SseEmitter.event()
                        .name("error")
                        .data("生成失败,请稍后重试"));
                    emitter.complete();
                } catch (IOException ex) {
                    emitter.completeWithError(ex);
                }
            }
        });

        return emitter;
    }

    /*@RequestMapping("/chat")
    public String chat(String message){
        String result = consultantService.chat(message);
        return result;
    }*/


    /*@Autowired
    private OpenAiChatModel model;
    @RequestMapping("/chat")
    public String chat(String message){//浏览器传递的用户问题
        String result = model.chat(message);
        return result;
    }*/

}
