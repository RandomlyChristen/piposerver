package com.mobilex.piposerver.batch.job;

import com.mobilex.piposerver.batch.tasklet.TempDeletionTasklet;
import com.mobilex.piposerver.service.PipoService;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class TempDeletionConfig {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;

    @Autowired
    private PipoService pipoService;

    @Bean
    public Job tempDeletionJob() {
        return jobBuilderFactory.get("tempDeletionJob")
                .start(tempDeletionStep())
                .build();
    }

    @Bean
    public Step tempDeletionStep() {
        return stepBuilderFactory.get("tempDeletionStep")
                .tasklet(new TempDeletionTasklet(pipoService))
                .build();
    }

}
