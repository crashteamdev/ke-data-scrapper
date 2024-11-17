package dev.crashteam.ke_data_scrapper.metric.timer;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@RequiredArgsConstructor
public abstract class BasicTimer {

    private final MeterRegistry meterRegistry;
    private final String METRIC_NAME;

    protected synchronized Timer findOrCreate(Tags tags) {
        return Optional.ofNullable(findTimer(tags)).orElseGet(() -> initTimer(tags));
    }

    protected synchronized Timer findOrCreate() {
        return Optional.ofNullable(findTimer()).orElseGet(this::initTimer);
    }

    private Timer findTimer(Tags tags) {
        return meterRegistry.find(METRIC_NAME).tags(tags).timer();
    }

    private Timer findTimer() {
        return meterRegistry.find(METRIC_NAME).timer();
    }

    private Timer initTimer() {
        return Timer.builder(METRIC_NAME).register(meterRegistry);
    }

    private Timer initTimer(Tags tags) {
        return Timer.builder(METRIC_NAME).tags(tags).register(meterRegistry);
    }
}
