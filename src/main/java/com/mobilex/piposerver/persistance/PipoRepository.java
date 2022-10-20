package com.mobilex.piposerver.persistance;

import com.mobilex.piposerver.model.PipoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface PipoRepository extends JpaRepository<PipoEntity, String> {
    List<PipoEntity> findAllByTempTrueAndCreatedAtLessThan(LocalDateTime lessThanDateTime);
    List<PipoEntity> findAllByUserId(String userId);
}
