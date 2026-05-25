package com.civiccms.sse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages SSE (Server-Sent Events) connections and broadcasts events
 * to all connected admin clients in real time.
 */
@Component
public class SsePublisher {

    private static final Logger log = LoggerFactory.getLogger(SsePublisher.class);

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    /**
     * Register a new SSE client connection.
     * @return SseEmitter to be returned from the controller
     */
    public SseEmitter register() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(()    -> emitters.remove(emitter));
        emitter.onError(e      -> emitters.remove(emitter));

        emitters.add(emitter);
        log.debug("SSE client connected. Total clients: {}", emitters.size());
        return emitter;
    }

    /**
     * Broadcast a named event with JSON data to all connected clients.
     * @param eventName e.g. "chaos_alert", "sla_breach", "status_update"
     * @param jsonData  raw JSON string payload
     */
    public void broadcast(String eventName, String jsonData) {
        List<SseEmitter> dead = new CopyOnWriteArrayList<>();

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(
                    SseEmitter.event()
                              .name(eventName)
                              .data(jsonData)
                );
            } catch (IOException e) {
                dead.add(emitter);
                log.debug("SSE client disconnected during broadcast, removing.");
            }
        }
        emitters.removeAll(dead);
    }

    /** @return number of currently connected SSE clients */
    public int connectedCount() {
        return emitters.size();
    }
}
