package com.mobilex.piposerver.batch.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class TempDeletionScheduler {
    private final long DELAY = 60 * 60 * 1000L;

    private final Job job;
    private final JobLauncher jobLauncher;

    @Scheduled(fixedDelay = DELAY)
    public void executeJob() {
        try {
            jobLauncher.run(
                    job,
                    new JobParametersBuilder()
                            .addString("datetime", LocalDateTime.now().toString())
                            .toJobParameters()
            );
        } catch (JobExecutionException ex) {
            System.out.println(ex.getMessage());
            ex.printStackTrace();
        }
    }
}
