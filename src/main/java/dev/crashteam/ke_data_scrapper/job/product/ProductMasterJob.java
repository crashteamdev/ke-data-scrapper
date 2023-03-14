package dev.crashteam.ke_data_scrapper.job.product;

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
public class ProductMasterJob implements Job {

    private final SimpleTriggerJobCreatorService creatorService;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        creatorService.createJob(Constant.PRODUCT_JOB_NAME, Constant.CATEGORY_ID_KEY, ProductJob.class, false);
    }
}

