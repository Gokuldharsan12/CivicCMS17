package com.civiccms.controller;

import com.civiccms.sse.SsePublisher;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Exposes a Server-Sent Events stream for real-time admin dashboard updates.
 * Clients connect to GET /api/v1/sse/stream and receive named events
 * such as "chaos_alert" and "sla_breach".
 */
@RestController
@RequestMapping("/api/v1/sse")
public class SseController {

    private final SsePublisher ssePublisher;

    public SseController(SsePublisher ssePublisher) {
        this.ssePublisher = ssePublisher;
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        return ssePublisher.register();
    }
}
