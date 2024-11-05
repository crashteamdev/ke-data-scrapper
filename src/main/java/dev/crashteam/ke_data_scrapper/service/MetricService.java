package dev.crashteam.ke_data_scrapper.service;

import dev.crashteam.ke_data_scrapper.metric.counter.JobErrorCounter;
import dev.crashteam.ke_data_scrapper.metric.counter.JobFinishedCounter;
import dev.crashteam.ke_data_scrapper.metric.timer.ResponseTimeRecorder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MetricService {

    private final JobFinishedCounter jobFinishedCounter;
    private final JobErrorCounter jobErrorCounter;
    private final ResponseTimeRecorder responseTimeRecorder;

    public void incrementFinishJob(String jobType) {
        jobFinishedCounter.increment(jobType);
    }

    public void incrementErrorJob(String jobType) {
        jobErrorCounter.increment(jobType);
    }

    public void recordResponseTime(Long timeMillis, String requestType) {
        responseTimeRecorder.record(timeMillis, requestType);
    }
}
