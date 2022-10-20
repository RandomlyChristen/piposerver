package com.mobilex.piposerver.batch.tasklet;

import com.mobilex.piposerver.model.PipoEntity;
import com.mobilex.piposerver.service.PipoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Component
public class TempDeletionTasklet implements Tasklet {

    private final PipoService pipoService;

    @Autowired
    public TempDeletionTasklet(PipoService pipoService) {
        this.pipoService = pipoService;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        LocalDateTime nowMinusAnHour = LocalDateTime.now(ZoneId.of("Asia/Seoul")).minusHours(1);
        List<PipoEntity> entities = pipoService.deleteTempEntities(nowMinusAnHour);
        log.info("Batch Job removes temp entities");
        for (PipoEntity entity : entities) {
            log.info("\t{}", entity.getId());
            pipoService.deleteFileFromEntity(entity);
        }
        return RepeatStatus.FINISHED;
    }
}
