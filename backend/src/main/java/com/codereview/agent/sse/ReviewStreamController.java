package com.codereview.agent.sse;

import com.codereview.agent.kafka.event.ReviewEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Connects Kafka review-events to browser clients via Server-Sent Events.
 *
 * Clients GET /api/reviews/{jobId}/stream and keep the connection open.
 * The controller listens to the Kafka topic, filters events by jobId, and
 * pushes them down the SSE emitter.
 */
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@Slf4j
public class ReviewStreamController {

    // Per-job fan-out — multiple dashboard tabs can watch the same review.
    private final Map<UUID, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();
    private final ObjectMapper mapper;

    @GetMapping(value = "/{jobId}/stream", produces = "text/event-stream")
    public SseEmitter stream(@PathVariable UUID jobId) {
        SseEmitter emitter = new SseEmitter(0L); // no timeout
        emitters.computeIfAbsent(jobId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> remove(jobId, emitter));
        emitter.onTimeout(() -> remove(jobId, emitter));
        emitter.onError(e -> remove(jobId, emitter));

        try {
            emitter.send(SseEmitter.event().name("connected").data(jobId.toString()));
        } catch (IOException ignored) {}
        return emitter;
    }

    @KafkaListener(topics = "${kafka-topics.review-events}", groupId = "sse-${random.uuid}")
    public void onReviewEvent(ReviewEvent event) {
        CopyOnWriteArrayList<SseEmitter> subscribers = emitters.get(event.jobId());
        if (subscribers == null || subscribers.isEmpty()) return;
        for (SseEmitter e : subscribers) {
            try {
                e.send(SseEmitter.event()
                        .name(event.type().name())
                        .data(mapper.writeValueAsString(event)));
            } catch (IOException ex) {
                log.debug("SSE client disconnected, removing");
                remove(event.jobId(), e);
            }
        }
    }

    private void remove(UUID jobId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> list = emitters.get(jobId);
        if (list != null) list.remove(emitter);
    }
}
