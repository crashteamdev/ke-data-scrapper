package dev.crashteam.ke_data_scrapper.metric.counter;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@RequiredArgsConstructor
public abstract class BasicCounter {
    private final MeterRegistry meterRegistry;
    private final String METRIC_NAME;

    protected synchronized Counter findOrCreate(Tags tags) {
        return Optional.ofNullable(findCounter(tags)).orElseGet(() -> initCounter(tags));
    }

    private Counter findCounter(Tags tags) {
        return meterRegistry.find(METRIC_NAME).tags(tags).counter();
    }

    private Counter initCounter(Tags tags) {
        return Counter.builder(METRIC_NAME).tags(tags).register(meterRegistry);
    }
}
