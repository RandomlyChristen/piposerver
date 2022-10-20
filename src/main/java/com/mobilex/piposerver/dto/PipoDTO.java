package com.mobilex.piposerver.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.mobilex.piposerver.model.PipoEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class PipoDTO {
    private String id;
    private String userId;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss[.SSS][.SS][.S]", timezone = "Asia/Seoul")
    private LocalDateTime createdAt;
    private boolean temp;
    private boolean processed;
    private String type;
    private int difficulty;

    public PipoEntity toEntity() {
        return PipoEntity.builder()
                .id(id)
                .userId(userId)
                .createdAt(createdAt)
                .temp(temp)
                .processed(processed)
                .type(type)
                .difficulty(difficulty)
                .build();
    }
}
