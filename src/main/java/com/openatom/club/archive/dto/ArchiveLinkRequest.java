package com.openatom.club.archive.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ArchiveLinkRequest {
    @NotBlank(message = "资料名称不能为空")
    private String title;

    @NotNull(message = "年份不能为空")
    private Integer archiveYear;

    private String archiveType;

    @NotBlank(message = "网盘链接不能为空")
    private String url;

    private String description;
}
