package com.openatom.club.homework.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class BatchDeleteHomeworkRequest {
    @NotEmpty(message = "请选择要删除的作业")
    private List<Long> ids;
}
