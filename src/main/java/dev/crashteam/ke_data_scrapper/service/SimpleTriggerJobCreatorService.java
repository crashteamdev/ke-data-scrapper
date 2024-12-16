package dev.crashteam.ke_data_scrapper.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.crashteam.ke_data_scrapper.model.ke.KeCategory;
import dev.crashteam.ke_data_scrapper.service.integration.KeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;

import static org.quartz.SimpleTrigger.MISFIRE_INSTRUCTION_FIRE_NOW;

@Slf4j
@Service
@RequiredArgsConstructor
public class SimpleTriggerJobCreatorService {

    private final KeService keService;
    private final Scheduler scheduler;

    public void createJob(String jobName, String idKey, Class<? extends Job> jobClass, boolean allIds) {

        Set<Long> ids;
        List<Long> rootIds = Collections.emptyList();
        if (!allIds) {
            ids = keService.getIds(false);
        } else {
            ids = keService.getAllIds();
            rootIds = keService.getRootCategories().stream().map(KeCategory.Data::getId).toList();
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
            if (!CollectionUtils.isEmpty(rootIds)
                    && rootIds.stream().anyMatch(it -> it.equals(categoryId))) {
                factoryBean.setPriority(10);
            }

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

        rootIdsMap.forEach((categoryId, children) -> {
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
                if (exists) {
                    scheduler.interrupt(jobKey);
                    Thread.sleep(10000L);
                    if (scheduler.checkExists(jobKey)) {
                        scheduler.deleteJob(jobKey);
                    }
                }
                scheduler.scheduleJob(jobDetail, factoryBean.getObject());
            } catch (SchedulerException e) {
                log.warn("Scheduler exception occurred with message: {}", e.getMessage());
            } catch (Exception e) {
                log.error("Failed to start job with exception ", e);
            }
        });

    }

}
