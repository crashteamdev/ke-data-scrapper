package dev.crashteam.ke_data_scrapper.metric.counter;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.springframework.stereotype.Component;

@Component
public class JobErrorCounter extends BasicCounter {
    public JobErrorCounter(MeterRegistry meterRegistry) {
        super(meterRegistry, "mm_parsing_category_job_error");
    }

    public void increment(String jobType) {
        findOrCreate(Tags.of("job_type", jobType)).increment();
    }
}
