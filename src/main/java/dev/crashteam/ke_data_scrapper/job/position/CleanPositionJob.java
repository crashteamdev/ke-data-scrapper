package dev.crashteam.ke_data_scrapper.job.position;

import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Slf4j
@Component
public class CleanPositionJob implements Job {

    @Autowired
    public Scheduler scheduler;

    @Autowired
    public JdbcTemplate jdbcTemplate;

    private static final String CALL_DELETE_POSITION_JOBS =
            "CALL deletePositionJobs()";

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        log.info("Deleting position jobs...");
        try {
            for (JobExecutionContext currentlyExecutingJob : scheduler.getCurrentlyExecutingJobs()) {
                scheduler.interrupt(currentlyExecutingJob.getJobDetail().getKey());
                Thread.sleep(20000L);
            }
            deletePositionJobs(4);
        } catch (Exception e) {
            log.error("Failed to interrupt executing jobs with exception ", e);
        }
    }

    private void deletePositionJobs(int attempt) {
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
            deletePositionJobs(attempt);
        }
     }
}
