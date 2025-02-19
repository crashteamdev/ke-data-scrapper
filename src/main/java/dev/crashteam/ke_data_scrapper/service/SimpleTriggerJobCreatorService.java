package dev.crashteam.ke_data_scrapper.service;

import dev.crashteam.ke_data_scrapper.service.integration.KeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import static org.quartz.SimpleTrigger.MISFIRE_INSTRUCTION_FIRE_NOW;

@Slf4j
@Service
@RequiredArgsConstructor
public class SimpleTriggerJobCreatorService {

    private final KeService keService;
    private final Scheduler scheduler;
    private final JdbcTemplate jdbcTemplate;

    private static final String CALL_DELETE_POSITION_JOBS =
            "CALL deleteProductJobs()";

    public void createJob(String jobName, String idKey, Class<? extends Job> jobClass, boolean allIds) {

        Set<Long> ids;
        deleteProductJobs(6);
        if (!allIds) {
            ids = keService.getIds(false);
        } else {
            ids = keService.getAllIds();
        }
        for (Long categoryId : ids) {
            String name = jobName.formatted(categoryId);
            JobKey jobKey = new JobKey(name);
            JobDetail jobDetail = JobBuilder.newJob(jobClass)
                    .withIdentity(jobKey).build();
            jobDetail.getJobDataMap().put(idKey, String.valueOf(categoryId));

            SimpleTriggerFactoryBean factoryBean = new SimpleTriggerFactoryBean();
            factoryBean.setStartTime(new Date());
            factoryBean.setStartDelay(0L);
            factoryBean.setRepeatInterval(10000L);
            factoryBean.setRepeatCount(0);
            factoryBean.setName(name);
            factoryBean.setMisfireInstruction(MISFIRE_INSTRUCTION_FIRE_NOW);
            factoryBean.afterPropertiesSet();

            try {
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

    public void createLightJob(String jobName, String idKey, Class<? extends Job> jobClass) {

        Map<Long, Set<Long>> rootIdsMap = keService.getRootIdsMap();
        log.info("Creating light job for {}", jobClass.getName());
        try {
            for (JobExecutionContext currentlyExecutingJob : scheduler.getCurrentlyExecutingJobs()) {
                try {
                    scheduler.interrupt(currentlyExecutingJob.getJobDetail().getKey());
                    Thread.sleep(20000L);
                } catch (Exception e) {
                    log.error("Failed to interrupt executing jobs with exception ", e);
                }
            }
        } catch (Exception e) {
            log.error("Failed to interrupt executing jobs with exception ", e);
        }
        rootIdsMap.forEach((categoryId, children) -> {
            String name = jobName.formatted(categoryId);
            JobKey jobKey = new JobKey(name);
            JobDetail jobDetail = JobBuilder.newJob(jobClass)
                    .withIdentity(jobKey)
                    .requestRecovery(true)
                    .build();
            jobDetail.getJobDataMap().put(idKey, String.valueOf(categoryId));

            SimpleTriggerFactoryBean factoryBean = new SimpleTriggerFactoryBean();
            factoryBean.setStartTime(new Date());
            factoryBean.setStartDelay(0L);
            factoryBean.setRepeatInterval(10000L);
            factoryBean.setRepeatCount(0);
            factoryBean.setName(name);
            factoryBean.setMisfireInstruction(MISFIRE_INSTRUCTION_FIRE_NOW);
            factoryBean.afterPropertiesSet();
            try {
                scheduler.scheduleJob(jobDetail, factoryBean.getObject());
            } catch (SchedulerException e) {
                log.warn("Scheduler exception occurred with message: {}", e.getMessage());
            } catch (Exception e) {
                log.error("Failed to start job with exception ", e);
            }
        });

    }

    private void deleteProductJobs(int attempt) {
        if (attempt == 0) return;
        try {
            jdbcTemplate.call(con -> con.prepareCall(CALL_DELETE_POSITION_JOBS), Collections.emptyList());
        } catch (Exception e) {
            log.error("Error while deleting position jobs, retrying", e);
            try {
                Thread.sleep(4000L);
            } catch (InterruptedException ex) {
                log.error("Interrupt exception", ex);
            }
            attempt = attempt - 1;
            deleteProductJobs(attempt);
        }
    }

}
