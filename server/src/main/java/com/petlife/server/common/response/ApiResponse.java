package com.petlife.server.common.response;

/**
 * 统一接口响应对象。
 *
 * @param <T> 响应数据类型
 */
public final class ApiResponse<T> {

    private final String code;
    private final String message;
    private final T data;

    private ApiResponse(String code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(ResponseCode.OK.getCode(), ResponseCode.OK.getMessage(), data);
    }

    public static ApiResponse<Void> failure(ResponseCode responseCode) {
        return new ApiResponse<>(responseCode.getCode(), responseCode.getMessage(), null);
    }

    public static ApiResponse<Void> failure(ResponseCode responseCode, String message) {
        return new ApiResponse<>(responseCode.getCode(), message, null);
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }
}
