package com.navigation.controller;

import com.navigation.aiservice.ConsultantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
// import reactor.core.publisher.Flux; // 暂时不使用流式输出

@RestController
@RequestMapping("/AI")
public class ChatController {
    @Autowired
    private ConsultantService consultantService;

    // 非流式版本 - 避免LangChain4J 0.30.0的Flux bug
    @GetMapping(value = "/chat", produces = "application/json;charset=utf-8")
    public String chat(@RequestParam("memoryId") String memoryId, @RequestParam("message") String message){
        return consultantService.chat(memoryId, message);
    }

    // 流式版本 - 暂时注释
    // @GetMapping(value = "/chatStream", produces = "text/event-stream;charset=utf-8")
    // public Flux<String> chatStream(@RequestParam("memoryId") String memoryId, @RequestParam("message") String message){
    //     return consultantService.chatStream(memoryId,message);
    // }

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
