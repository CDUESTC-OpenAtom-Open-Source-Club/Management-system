package com.openatom.club.auth.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponse {
    private String token;
    private CurrentUserResponse user;
}
