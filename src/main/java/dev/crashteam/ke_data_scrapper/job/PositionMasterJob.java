package dev.crashteam.ke_data_scrapper.job;

import dev.crashteam.ke_data_scrapper.model.Constant;
import dev.crashteam.ke_data_scrapper.service.SimpleTriggerJobCreatorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PositionMasterJob implements Job {

    private final SimpleTriggerJobCreatorService creatorService;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        creatorService.createJob(Constant.POSITION_JOB_NAME, Constant.POSITION_CATEGORY_KEY, PositionJob.class);
    }
}
