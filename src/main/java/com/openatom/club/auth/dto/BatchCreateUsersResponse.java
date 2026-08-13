package com.openatom.club.auth.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class BatchCreateUsersResponse {
    private List<BatchCreateSuccessItem> created;
    private List<BatchCreateFailedItem> failed;

    @Data
    @Builder
    public static class BatchCreateSuccessItem {
        private String username;
        private Long memberId;
    }

    @Data
    @Builder
    public static class BatchCreateFailedItem {
        private String username;
        private String reason;
    }
}
