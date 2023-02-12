package dev.crashteam.ke_data_scrapper.configuration;

import dev.crashteam.ke_data_scrapper.job.CategoryGqlJob;
import dev.crashteam.ke_data_scrapper.model.Constant;
import dev.crashteam.ke_data_scrapper.service.integration.KeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean;

import javax.annotation.PostConstruct;
import java.util.Date;

import static org.quartz.SimpleTrigger.MISFIRE_INSTRUCTION_FIRE_NOW;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class JobConfiguration {

    private final KeService keService;
    private final ApplicationContext applicationContext;

    @Async
    //@PostConstruct
    public void createCategoryJobs() {
        for (Long categoryId : keService.getIds()) {
            String jobName = "%s-position-product-job".formatted(categoryId);
            JobKey jobKey = new JobKey(jobName);
            JobDetail jobDetail = JobBuilder.newJob(CategoryGqlJob.class)
                    .withIdentity(jobKey).build();
            jobDetail.getJobDataMap().put(Constant.CATEGORY_ID_KEY, categoryId);

            SimpleTriggerFactoryBean factoryBean = new SimpleTriggerFactoryBean();
            factoryBean.setStartTime(new Date());
            factoryBean.setStartDelay(0L);
            factoryBean.setRepeatInterval(10000L);
            factoryBean.setRepeatCount(0);
            factoryBean.setName(jobName);
            factoryBean.setMisfireInstruction(MISFIRE_INSTRUCTION_FIRE_NOW);
            factoryBean.afterPropertiesSet();

            try {
                Scheduler scheduler = applicationContext.getBean(Scheduler.class);
                boolean exists = scheduler.checkExists(jobKey);
                if (exists) continue;
                scheduler.scheduleJob(jobDetail, factoryBean.getObject());
            } catch (SchedulerException e) {
                log.warn("Scheduler exception occurred with message: {}", e.getMessage());
            } catch (Exception e) {
                log.error("Failed to start job with exception ", e);
            }
        }
    }
}

