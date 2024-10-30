package dev.crashteam.ke_data_scrapper.service;

import dev.crashteam.ke_data_scrapper.metric.counter.JobErrorCounter;
import dev.crashteam.ke_data_scrapper.metric.counter.JobFinishedCounter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MetricService {

    private final JobFinishedCounter jobFinishedCounter;
    private final JobErrorCounter jobErrorCounter;

    public void incrementFinishJob(String jobType) {
        jobFinishedCounter.increment(jobType);
    }

    public void incrementErrorJob(String jobType) {
        jobErrorCounter.increment(jobType);
    }
}
