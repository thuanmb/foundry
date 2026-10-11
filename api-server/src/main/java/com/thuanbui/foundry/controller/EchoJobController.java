package com.thuanbui.foundry.controller;

import com.thuanbui.foundry.common.model.entity.AsyncJob;
import com.thuanbui.foundry.common.model.enums.JobType;
import com.thuanbui.foundry.config.WorkerProperties;
import com.thuanbui.foundry.model.dto.EchoJobRequest;
import com.thuanbui.foundry.model.dto.JobCreatedResponse;
import com.thuanbui.foundry.security.FeatureFlag;
import com.thuanbui.foundry.security.RequestIdentity;
import com.thuanbui.foundry.security.UnauthorizedException;
import com.thuanbui.foundry.services.AsyncJobService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Demo async job: the worker echoes the message back after {@code delaySec}. */
@RestController
@RequestMapping("/api/v1/jobs")
public class EchoJobController extends BaseApiController {

    static final String JOB_NAME = "echo";

    private final AsyncJobService asyncJobService;
    private final WorkerProperties workerProperties;

    public EchoJobController(AsyncJobService asyncJobService, WorkerProperties workerProperties) {
        this.asyncJobService = asyncJobService;
        this.workerProperties = workerProperties;
    }

    @PostMapping("/echo")
    @FeatureFlag("echo")
    public ResponseEntity<JobCreatedResponse> createEchoJob(@Valid @RequestBody EchoJobRequest body,
            HttpServletRequest request) {
        String ownerKey = RequestIdentity.ownerKey(request);
        if (ownerKey == null) {
            throw new UnauthorizedException();
        }

        int delaySec = body.delaySec() != null ? body.delaySec() : 0;
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("message", body.message());
        parameters.put("delaySec", delaySec);

        AsyncJob job = asyncJobService.createJob(JobType.ECHO, JOB_NAME, parameters, ownerKey,
                delaySec + 1, 0, workerProperties.job().maxRetries());

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new JobCreatedResponse(job.getId().toString(), job.getStatus().name()));
    }
}
