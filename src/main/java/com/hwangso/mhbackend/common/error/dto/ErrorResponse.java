package com.hwangso.mhbackend.common.error.dto;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
        int status,
        String code,
        String message,
        String path,
        Instant timestamp,
        List<FieldViolation> violations // validation 에러용(없으면 null/empty)
) {
    public record FieldViolation(
            String field,
            String reason
    ) {}
}