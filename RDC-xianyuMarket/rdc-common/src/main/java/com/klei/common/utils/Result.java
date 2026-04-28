package com.klei.common.utils;

public class Result<T> {

    private int code;
    private String msg;
    private T data;

    private Result() {}

    private Result(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    // 成功，带数据
    public static <T> Result<T> ok(T data) {
        return new Result<>(200, "success", data);
    }

    // 成功，不带数据
    public static <T> Result<T> ok() {
        return new Result<>(200, "success", null);
    }

    // 业务失败
    public static <T> Result<T> fail(String msg) {
        return new Result<>(500, msg, null);
    }

    // 自定义状态码失败
    public static <T> Result<T> fail(int code, String msg) {
        return new Result<>(code, msg, null);
    }

    // 快捷方法：未登录
    public static <T> Result<T> unauthorized() {
        return new Result<>(401, "未登录或登录已过期", null);
    }

    // 快捷方法：无权限
    public static <T> Result<T> forbidden() {
        return new Result<>(403, "无权限访问", null);
    }

    public int getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }

    public T getData() {
        return data;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public void setData(T data) {
        this.data = data;
    }
}