package com.openatom.club.cohort.dto;

import com.openatom.club.cohort.entity.Cohort;
import lombok.Data;

@Data
public class CohortResponse {
    private Long id;
    private Integer year;
    private Boolean enabled;

    public static CohortResponse from(Cohort c) {
        CohortResponse dto = new CohortResponse();
        dto.id = c.getId();
        dto.year = c.getYear();
        dto.enabled = c.getEnabled();
        return dto;
    }
}
