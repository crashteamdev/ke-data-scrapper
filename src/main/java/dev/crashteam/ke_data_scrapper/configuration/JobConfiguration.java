package dev.crashteam.ke_data_scrapper.configuration;

import dev.crashteam.ke_data_scrapper.job.category.CategoryJob;
import dev.crashteam.ke_data_scrapper.job.position.PositionMasterJob;
import dev.crashteam.ke_data_scrapper.job.product.ProductMasterJob;
import dev.crashteam.ke_data_scrapper.job.trim.TrimJob;
import dev.crashteam.ke_data_scrapper.model.Constant;
import dev.crashteam.ke_data_scrapper.model.job.JobModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

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

    @Value("${app.job.cron.trim-job}")
    private String trimJobCron;

    @PostConstruct
    public void init() {
        scheduleJob(new JobModel("product-master-job", ProductMasterJob.class, productJobCron,
               Constant.PRODUCT_MASTER_JOB_TRIGGER, Constant.MASTER_JOB_GROUP));
        scheduleJob(new JobModel("position-master-job", PositionMasterJob.class, positionJobCron,
                Constant.POSITION_MASTER_JOB_TRIGGER, Constant.MASTER_JOB_GROUP));
        scheduleJob(new JobModel("category-master-job", CategoryJob.class, categoryJobCron,
                Constant.CATEGORY_MASTER_JOB_TRIGGER, Constant.MASTER_JOB_GROUP));
        scheduleJob(new JobModel("trim-job", TrimJob.class, trimJobCron,
                Constant.TRIM_MASTER_JOB_TRIGGER, Constant.MASTER_JOB_GROUP));
    }

    private void scheduleJob(JobModel jobModel) {
        try {
            JobDetail jobDetail = getJobDetail(jobModel.getJobName(), jobModel.getJobClass());
            scheduler.addJob(jobDetail, true, true);
            if (!scheduler.checkExists(TriggerKey.triggerKey(jobModel.getTriggerName(), jobModel.getTriggerGroup()))) {
                scheduler.scheduleJob(getJobTrigger(jobDetail, jobModel.getCron(), jobModel.getTriggerName(),  jobModel.getTriggerGroup()));
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
                .withSchedule(CronScheduleBuilder.cronSchedule(cron))
                .withIdentity(TriggerKey.triggerKey(name, group))
                .forJob(jobDetail).build();
    }
}
