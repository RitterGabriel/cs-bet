package com.csbet.backend.market;

import com.csbet.backend.pricing.PriceScheduler;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.*;
import org.springframework.scheduling.annotation.*;
import org.springframework.scheduling.config.FixedDelayTask;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PriceSchedulerRegistrationTest {
    @Configuration(proxyBeanMethods = false)
    @EnableScheduling
    @Import(PriceScheduler.class)
    static class ScheduledConfig { }
    private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
            .withUserConfiguration(ScheduledConfig.class)
            .withBean(MarketRefreshService.class, () -> mock(MarketRefreshService.class))
            .withPropertyValues("csbet.market.enabled=${MARKET_QUOTES_ENABLED:false}");

    @Test
    void enabledEnvironmentRegistersAnHourlyTaskInTheWebApplication() {
        runner.withPropertyValues("MARKET_QUOTES_ENABLED=true").run(context -> {
            assertThat(context).hasSingleBean(PriceScheduler.class);
            var tasks = context.getBean(ScheduledAnnotationBeanPostProcessor.class).getScheduledTasks();
            assertThat(tasks).hasSize(1);
            assertThat(tasks.iterator().next().getTask()).isInstanceOf(FixedDelayTask.class);
            var task = (FixedDelayTask) tasks.iterator().next().getTask();
            assertThat(task.getIntervalDuration()).isEqualTo(Duration.ofHours(1));
        });
    }
    @Test
    void disabledEnvironmentDoesNotRegisterTheTask() {
        runner.withPropertyValues("MARKET_QUOTES_ENABLED=false").run(context -> {
            assertThat(context).doesNotHaveBean(PriceScheduler.class);
            assertThat(context.getBean(ScheduledAnnotationBeanPostProcessor.class).getScheduledTasks()).isEmpty();
            verifyNoInteractions(context.getBean(MarketRefreshService.class));
        });
    }
}
