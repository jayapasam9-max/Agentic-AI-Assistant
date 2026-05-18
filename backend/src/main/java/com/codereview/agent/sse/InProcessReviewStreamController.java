package com.codereview.agent.sse;

import com.codereview.agent.kafka.event.ReviewEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * In-process counterpart to {@link ReviewStreamController}.
 *
 * <p>Activated when {@code review-bus.type=in-process} (the {@code cloud-free}
 * profile used on the Render free-tier deploy). Instead of consuming a Kafka
 * topic, this controller listens for {@link ReviewEvent} on Spring's in-process
 * event bus and fans events out to subscribed dashboard clients via SSE.
 *
 * <p>The two controllers are gated on mutually-exclusive
 * {@code review-bus.type} values, so they never both register — and they share
 * the same URL ({@code GET /api/public/reviews/{jobId}/stream}) so the
 * frontend is agnostic to which profile the backend is running.
 */
@RestController
@ConditionalOnProperty(name = "review-bus.type", havingValue = "in-process")
@RequestMapping("/api/public/reviews")
@RequiredArgsConstructor
@Slf4j
public class InProcessReviewStreamController {

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
        } catch (IOException ignored) {
        }
        return emitter;
    }

    @EventListener
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
