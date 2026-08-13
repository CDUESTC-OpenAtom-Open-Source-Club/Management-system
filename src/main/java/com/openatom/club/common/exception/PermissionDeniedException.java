package com.openatom.club.common.exception;

public class PermissionDeniedException extends RuntimeException {

    public PermissionDeniedException(String message) {
        super(message);
    }

    public static PermissionDeniedException noPermission() {
        return new PermissionDeniedException("权限不足，无法执行此操作");
    }

    public static PermissionDeniedException of(String message) {
        return new PermissionDeniedException(message);
    }
}
