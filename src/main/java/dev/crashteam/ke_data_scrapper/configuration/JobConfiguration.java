package dev.crashteam.ke_data_scrapper.configuration;

import dev.crashteam.ke_data_scrapper.job.CacheHandler;
import dev.crashteam.ke_data_scrapper.job.category.CategoryJob;
import dev.crashteam.ke_data_scrapper.job.position.PositionMasterJob;
import dev.crashteam.ke_data_scrapper.job.product.ProductMasterJob;
import dev.crashteam.ke_data_scrapper.model.Constant;
import dev.crashteam.ke_data_scrapper.model.job.JobModel;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.TimeZone;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class JobConfiguration {

    private final Scheduler scheduler;

    @Value("${app.job.cron.product-job}")
    private String productJobCron;

    @Value("${app.job.cron.position-job}")
    private String positionJobCron;

    @Value("${app.job.cron.category-job}")
    private String categoryJobCron;

    @Value("${app.job.cron.delete-product-cache}")
    private String deleteProductCache;

    @PostConstruct
    public void init() {
        scheduleJob(new JobModel(Constant.PRODUCT_MASTER_JOB_NAME, ProductMasterJob.class, productJobCron,
                Constant.PRODUCT_MASTER_JOB_TRIGGER, Constant.MASTER_JOB_GROUP));
        scheduleJob(new JobModel(Constant.POSITION_MASTER_JOB_NAME, PositionMasterJob.class, positionJobCron,
                Constant.POSITION_MASTER_JOB_TRIGGER, Constant.MASTER_JOB_GROUP));
        scheduleJob(new JobModel(Constant.CATEGORY_MASTER_JOB_NAME, CategoryJob.class, categoryJobCron,
                Constant.CATEGORY_MASTER_JOB_TRIGGER, Constant.MASTER_JOB_GROUP));
        scheduleJob(new JobModel(Constant.DELETE_PRODUCT_CACHE_JOB_NAME, CacheHandler.class, deleteProductCache,
                Constant.DELETE_PRODUCT_CACHE_TRIGGER_NAME, "cache"));

    }

    private void scheduleJob(JobModel jobModel) {
        try {
            JobDetail jobDetail = getJobDetail(jobModel.getJobName(), jobModel.getJobClass());
            scheduler.addJob(jobDetail, true, true);
            if (!scheduler.checkExists(TriggerKey.triggerKey(jobModel.getTriggerName(), jobModel.getTriggerGroup()))) {
                scheduler.scheduleJob(getJobTrigger(jobDetail, jobModel.getCron(), jobModel.getTriggerName(), jobModel.getTriggerGroup()));
                log.info("Scheduled - {} with cron - {}", jobModel.getJobName(), jobModel.getCron());
            }
        } catch (SchedulerException e) {
            log.warn("Scheduler exception occurred with message: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Failed to start job with exception ", e);
        }
    }

    private JobDetail getJobDetail(String jobName, Class<? extends Job> jobClass) {
        JobKey jobKey = new JobKey(jobName);
        return JobBuilder.newJob(jobClass)
                .withIdentity(jobKey).build();
    }

    private CronTrigger getJobTrigger(JobDetail jobDetail, String cron, String name, String group) {
        return TriggerBuilder
                .newTrigger()
                .withSchedule(CronScheduleBuilder
                        .cronSchedule(cron)
                        .withMisfireHandlingInstructionFireAndProceed()
                        .inTimeZone(TimeZone.getTimeZone("UTC")))
                .withIdentity(TriggerKey.triggerKey(name, group))
                .forJob(jobDetail).build();
    }
}
