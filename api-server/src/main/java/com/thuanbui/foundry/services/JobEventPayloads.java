package com.thuanbui.foundry.services;

import com.thuanbui.foundry.common.model.entity.AsyncJob;
import java.util.LinkedHashMap;
import java.util.Map;

/** Builds the JSON payloads shared by the job status endpoint and SSE events. */
public final class JobEventPayloads {

    public static final String EVENT_STATUS = "status";
    public static final String EVENT_COMPLETED = "completed";
    public static final String EVENT_FAILED = "failed";

    private JobEventPayloads() {
    }

    public static Map<String, Object> status(AsyncJob job) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("jobId", job.getId().toString());
        data.put("status", job.getStatus().name());
        if (job.getEstimatedDurationSec() != null) {
            data.put("estimatedWaitSec", job.getEstimatedDurationSec());
        }
        return data;
    }

    public static Map<String, Object> completed(AsyncJob job) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("jobId", job.getId().toString());
        data.put("result", job.getResult());
        return data;
    }

    public static Map<String, Object> failed(AsyncJob job) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("jobId", job.getId().toString());
        data.put("error", error(job));
        return data;
    }

    public static Map<String, String> error(AsyncJob job) {
        Map<String, String> error = new LinkedHashMap<>();
        error.put("code", "JOB_FAILED");
        error.put("message", job.getMessage() != null ? job.getMessage() : "Job processing failed");
        return error;
    }
}
