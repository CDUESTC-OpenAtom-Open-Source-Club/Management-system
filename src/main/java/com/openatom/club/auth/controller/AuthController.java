package com.openatom.club.auth.controller;

import com.openatom.club.auth.dto.*;
import com.openatom.club.auth.service.AuthService;
import com.openatom.club.common.response.ApiResponse;
import com.openatom.club.common.response.PageResult;
import com.openatom.club.common.security.ActorHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "认证管理")
@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "登录")
    @PostMapping("/api/auth/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        return ApiResponse.success(authService.login(req));
    }

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/api/auth/me")
    public ApiResponse<CurrentUserResponse> me() {
        return ApiResponse.success(authService.getCurrentUser(ActorHolder.get().getUserId()));
    }

    @Operation(summary = "修改密码")
    @PutMapping("/api/auth/me/password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest req) {
        authService.changePassword(ActorHolder.get().getUserId(), req);
        return ApiResponse.success(null);
    }

    @GetMapping("/api/users")
    public ApiResponse<PageResult<UserAccountResponse>> listUsers(@RequestParam(required = false) String keyword,
                                                                  @RequestParam(defaultValue = "1") int page,
                                                                  @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(authService.listUsers(keyword, page, size));
    }

    @PostMapping("/api/users")
    public ApiResponse<UserAccountResponse> createUser(@Valid @RequestBody CreateUserRequest req) {
        return ApiResponse.success(authService.createUser(req));
    }

    @PostMapping("/api/users/batch")
    public ApiResponse<BatchCreateUsersResponse> batchCreateUsers(@Valid @RequestBody BatchCreateUsersRequest req) {
        return ApiResponse.success(authService.batchCreateUsers(req));
    }

    @PutMapping("/api/users/{id}/enabled")
    public ApiResponse<Void> updateEnabled(@PathVariable Long id, @RequestBody UpdateUserEnabledRequest req) {
        authService.updateEnabled(id, req.getEnabled());
        return ApiResponse.success(null);
    }

    @PostMapping("/api/users/{id}/reset-password")
    public ApiResponse<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody ResetPasswordRequest req) {
        authService.resetPassword(id, req.getNewPassword());
        return ApiResponse.success(null);
    }

    @DeleteMapping("/api/users/{id}")
    public ApiResponse<Void> deleteUser(@PathVariable Long id) {
        authService.deleteUser(id);
        return ApiResponse.success(null);
    }

    @GetMapping("/api/my/profile")
    public ApiResponse<CurrentUserResponse> getMyProfile() {
        return ApiResponse.success(authService.getMyProfile(ActorHolder.get().getUserId()));
    }

    @PutMapping("/api/my/profile")
    public ApiResponse<CurrentUserResponse> updateMyProfile(@RequestBody UpdateMyProfileRequest req) {
        return ApiResponse.success(authService.updateMyProfile(ActorHolder.get().getUserId(), req));
    }
}
