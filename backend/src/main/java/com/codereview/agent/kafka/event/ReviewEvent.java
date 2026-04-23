package com.codereview.agent.kafka.event;

import java.util.UUID;

/**
 * Emitted as the agent progresses. The SSE controller filters these by jobId
 * and streams them to the React dashboard in real time.
 */
public record ReviewEvent(
        UUID jobId,
        Type type,
        String payload
) {
    public enum Type {
        JOB_STARTED,
        REASONING_CHUNK,   // partial model output, for the "typing" effect
        TOOL_CALL_STARTED,
        TOOL_CALL_COMPLETED,
        FINDING_EMITTED,
        JOB_COMPLETED,
        JOB_FAILED
    }
}
