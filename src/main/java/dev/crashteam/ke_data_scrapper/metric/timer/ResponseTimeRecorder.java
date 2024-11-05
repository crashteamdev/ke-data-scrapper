package dev.crashteam.ke_data_scrapper.metric.timer;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class ResponseTimeRecorder extends BasicTimer {

    public ResponseTimeRecorder(MeterRegistry meterRegistry) {
        super(meterRegistry, "mm_response_timer");
    }

    public void record(Long timeMillis, String requestType) {
        findOrCreate(Tags.of("request_type", requestType)).record(timeMillis, TimeUnit.MILLISECONDS);
    }
}
