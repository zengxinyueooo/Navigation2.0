package com.navigation.controller.AI;

import com.navigation.result.Result;
import com.navigation.service.impl.AITravelSummaryService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/ai-travel")
@Slf4j
public class AITravelController {

    @Autowired
    private AITravelSummaryService aiTravelSummaryService;

    /**
     * 实时生成行程摘要
     */
    @GetMapping("/summary/real-time/{routeId}")
    public Result<String> generateSummaryRealTime(@PathVariable Long routeId) {
        try {
            long startTime = System.currentTimeMillis();
            String summary = aiTravelSummaryService.generateTravelSummaryRealTime(routeId);
            long costTime = System.currentTimeMillis() - startTime;

            log.info("AI摘要生成完成，耗时: {}ms", costTime);
            return Result.success(summary);

        } catch (Exception e) {
            log.error("生成行程摘要失败", e);
            return Result.error("AI服务暂时不可用，请稍后重试");
        }
    }

    /**
     * SSE流式生成行程摘要 - 新增功能
     */
                                                      //告诉Spring这个接口返回的是SSE事件流，而不是普通的JSON
    @GetMapping(value = "/summary/stream/{routeId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamTravelSummary(@PathVariable Long routeId) {
        // 创建SSE发射器，设置超时时间（5分钟）
        //SseEmitter是Spring提供的专门用于处理SSE的类，它封装了底层的事件推送逻辑。
        // 我们设置5分钟超时，因为AI生成可能需要一些时间
        SseEmitter emitter = new SseEmitter(5 * 60 * 1000L);

        log.info("开始SSE流式生成行程摘要，routeId: {}", routeId);

        // 异步处理，避免阻塞HTTP线程
        // 因为SSE连接需要保持长时间开放，如果我们同步处理，就会阻塞HTTP线程，导致服务器无法处理其他请求
        // 通过异步处理，我们立即返回emitter对象给前端建立连接，然后在后台线程中执行耗时的AI生成任务
        CompletableFuture.runAsync(() -> {
            try {
                aiTravelSummaryService.generateAndStreamSummary(routeId, emitter);
            } catch (Exception e) {
                log.error("SSE流式生成失败", e);
                emitter.completeWithError(e);
            }
        });


        // 设置完成和超时回调
        emitter.onCompletion(() -> log.info("SSE连接完成，routeId: {}", routeId));
        emitter.onTimeout(() -> log.info("SSE连接超时，routeId: {}", routeId));
        emitter.onError((e) -> log.error("SSE连接错误，routeId: {}", routeId, e));

        return emitter;
    }

    /**
     * 批量生成多个行程的摘要
     */
    @PostMapping("/summary/batch")
    public Result<List<RouteSummaryDTO>> generateBatchSummaries(@RequestBody List<Long> routeIds) {
        List<RouteSummaryDTO> results = new ArrayList<>();

        for (Long routeId : routeIds) {
            try {
                String summary = aiTravelSummaryService.generateTravelSummaryRealTime(routeId);
                results.add(new RouteSummaryDTO(routeId, summary, "success"));
            } catch (Exception e) {
                results.add(new RouteSummaryDTO(routeId, "生成失败", "error"));
            }
        }

        return Result.success(results);
    }
}

/**
 * 行程摘要DTO
 */
@Data
@AllArgsConstructor
class RouteSummaryDTO {
    private Long routeId;
    private String summary;
    private String status;
}
