package com.thuanbui.foundry.worker.job;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.thuanbui.foundry.common.model.entity.AsyncJob;
import com.thuanbui.foundry.common.model.enums.JobType;

@Component
public class JobDispatcher {

    private static final Logger log = LoggerFactory.getLogger(JobDispatcher.class);

    private final Map<JobType, JobProcessor> processors = new EnumMap<>(JobType.class);

    public JobDispatcher(List<JobProcessor> processorBeans) {
        for (JobProcessor processor : processorBeans) {
            JobProcessor existing = processors.putIfAbsent(processor.supportedType(), processor);
            if (existing != null) {
                throw new IllegalStateException("Multiple JobProcessors registered for " + processor.supportedType()
                        + ": " + existing.getClass().getName() + ", " + processor.getClass().getName());
            }
        }
        log.info("Registered job processors for types: {}", processors.keySet());
    }

    public Map<String, Object> dispatch(AsyncJob job) {
        JobType jobType = job.getJobType();
        JobProcessor processor = processors.get(jobType);
        if (processor == null) {
            throw new PermanentJobException("No processor available for job type: " + jobType);
        }
        log.info("Dispatching job {} of type {}", job.getId(), jobType);
        return processor.process(job);
    }
}
