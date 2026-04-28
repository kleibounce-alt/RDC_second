package com.klei.common.exception;


public class AuthException extends RuntimeException {

    // 401=未登录, 403=无权限
    private int code;

    public AuthException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}