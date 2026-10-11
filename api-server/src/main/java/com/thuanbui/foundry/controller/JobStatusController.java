package com.thuanbui.foundry.controller;

import com.thuanbui.foundry.common.model.entity.AsyncJob;
import com.thuanbui.foundry.common.model.enums.JobStatus;
import com.thuanbui.foundry.security.RequestIdentity;
import com.thuanbui.foundry.services.AsyncJobService;
import com.thuanbui.foundry.services.JobEventPayloads;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/jobs")
public class JobStatusController extends BaseApiController {

    private final AsyncJobService asyncJobService;

    public JobStatusController(AsyncJobService asyncJobService) {
        this.asyncJobService = asyncJobService;
    }

    @GetMapping("/{jobId}/status")
    public ResponseEntity<Map<String, Object>> getJobStatus(@PathVariable UUID jobId, HttpServletRequest request) {
        AsyncJob job = asyncJobService.getOwnedJob(jobId, RequestIdentity.ownerKey(request));

        Map<String, Object> response = JobEventPayloads.status(job);
        if (job.getStatus() == JobStatus.COMPLETED) {
            response.put("result", job.getResult());
        } else if (job.getStatus() == JobStatus.FAILED) {
            response.put("error", JobEventPayloads.error(job));
        }
        return ResponseEntity.ok(response);
    }
}
