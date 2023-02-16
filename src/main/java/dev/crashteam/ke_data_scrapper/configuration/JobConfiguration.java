package dev.crashteam.ke_data_scrapper.configuration;

import dev.crashteam.ke_data_scrapper.job.ProductMasterJob;
import dev.crashteam.ke_data_scrapper.model.Constant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.CronTriggerFactoryBean;

import javax.annotation.PostConstruct;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class JobConfiguration {

    private final Scheduler scheduler;

    @Value("${app.job.cron.product-job}")
    private String productJobCron;

    @PostConstruct
    public void init() {

        String jobName = "product-master-job";
        JobKey jobKey = new JobKey(jobName);
        JobDetail jobDetail = JobBuilder.newJob(ProductMasterJob.class)
                .withIdentity(jobKey).build();

        CronTriggerFactoryBean factoryBean = new CronTriggerFactoryBean();
        factoryBean.setMisfireInstruction(CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING);
        factoryBean.setCronExpression(productJobCron);
        CronTrigger cronTrigger = TriggerBuilder
                .newTrigger()
                .withSchedule(CronScheduleBuilder.cronSchedule(productJobCron))
                .withIdentity(TriggerKey.triggerKey(Constant.PRODUCT_MASTER_JOB_TRIGGER, Constant.MASTER_JOB_GROUP))
                .forJob(jobDetail).build();

        try {
            scheduler.addJob(jobDetail, true, true);
            if (!scheduler.checkExists(TriggerKey.triggerKey(Constant.PRODUCT_MASTER_JOB_TRIGGER, Constant.MASTER_JOB_GROUP))) {
                scheduler.scheduleJob(cronTrigger);
                log.info("Scheduled - {} with cron - {}", jobName, productJobCron);
            }
        } catch (SchedulerException e) {
            log.warn("Scheduler exception occurred with message: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Failed to start job with exception ", e);
        }

    }
}
