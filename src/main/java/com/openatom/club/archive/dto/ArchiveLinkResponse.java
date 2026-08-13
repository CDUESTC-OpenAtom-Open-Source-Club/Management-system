package com.openatom.club.archive.dto;

import com.openatom.club.archive.entity.ArchiveLink;
import lombok.Data;
import java.time.OffsetDateTime;

@Data
public class ArchiveLinkResponse {
    private Long id;
    private String title;
    private Integer archiveYear;
    private String archiveType;
    private String url;
    private String description;
    private String createdBy;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static ArchiveLinkResponse from(ArchiveLink a) {
        ArchiveLinkResponse dto = new ArchiveLinkResponse();
        dto.id = a.getId();
        dto.title = a.getTitle();
        dto.archiveYear = a.getArchiveYear();
        dto.archiveType = a.getArchiveType();
        dto.url = a.getUrl();
        dto.description = a.getDescription();
        dto.createdBy = a.getCreatedBy();
        dto.createdAt = a.getCreatedAt();
        dto.updatedAt = a.getUpdatedAt();
        return dto;
    }
}
