package com.housedesign.common;

import lombok.Data;

@Data
public class Result<T> {
    private T data;
    private String message;
    private int code;//

    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<T>();
        result.data = data;
        result.message = "success";
        result.code = 200;
        return result;
    }

    public static <T> Result<T> success() {
        Result<T> result = new Result<T>();
        result.message = "success";
        result.code = 200;
        return result;
    }

    public static <T> Result<T> error(int code, String message) {
        Result<T> result = new Result<T>();
        result.code = code;
        result.message = message;
        return result;
    }

    public static <T> Result<T> error(String message) {
        Result<T> result = new Result<T>();
        result.message = message;
        return result;
    }
}
