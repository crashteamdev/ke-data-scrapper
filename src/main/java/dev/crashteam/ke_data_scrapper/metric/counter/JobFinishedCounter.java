package dev.crashteam.ke_data_scrapper.metric.counter;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.springframework.stereotype.Component;

@Component
public class JobFinishedCounter extends BasicCounter {

    public JobFinishedCounter(MeterRegistry meterRegistry) {
        super(meterRegistry, "mm_finish_parsing_category_job");
    }

    public void increment(String jobType) {
        findOrCreate(Tags.of("job_type", jobType));
    }
}
