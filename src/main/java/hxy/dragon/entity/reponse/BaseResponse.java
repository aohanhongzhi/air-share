package hxy.dragon.entity.reponse;


import hxy.dragon.entity.enums.Enumerator;
import lombok.Data;

import java.io.Serializable;

/**
 * @param <T> 基础响应类
 * @author eric
 * @date 9/9/19 4:38 PM
 */
@Data
public class BaseResponse<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final int CODE_SUCCESS = 200;

    private static final int CODE_FAIL = 500;

    private static final int CODE_ERROR = 500;

    private static final int CODE_NOT_FOUND = 404;
    /**
     * 时间戳
     */
    private long timestamp = System.currentTimeMillis();

    private int code;

    private String msg;

    private T data;

    public BaseResponse() {
    }

    public BaseResponse(int code, String msg, T data) {
        this.setCode(code);
        this.setMsg(msg);
        this.setData(data);
    }

    public BaseResponse(Enumerator enumerator, T data) {
        this.setCode(enumerator.code());
        this.setMsg(enumerator.description());
        this.setData(data);
    }

    public static <T> BaseResponse<T> success() {
        return new BaseResponse<T>(CODE_SUCCESS, "success", null);
    }

    public static <T> BaseResponse<T> success(String message) {
        return new BaseResponse<T>(CODE_SUCCESS, message, null);
    }

    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<T>(CODE_SUCCESS, "success", data);
    }

    public static <T> BaseResponse<T> success(String message, T data) {
        return new BaseResponse<T>(CODE_SUCCESS, message, data);
    }

    public static <T> BaseResponse<T> error() {
        return new BaseResponse<T>(CODE_ERROR, "fail", null);
    }

    public static <T> BaseResponse<T> error(String message) {
        return new BaseResponse<T>(CODE_ERROR, message, null);
    }

    public static <T> BaseResponse<T> error(T data) {
        return new BaseResponse<T>(CODE_ERROR, "fail", data);
    }

    public static <T> BaseResponse<T> error(String message, T data) {
        return new BaseResponse<T>(CODE_ERROR, message, data);
    }

    public static <T> BaseResponse<T> badrequest() {
        return new BaseResponse<T>(CODE_FAIL, "no identifier arguments", null);
    }

    public static <T> BaseResponse<T> badrequest(String message) {
        return new BaseResponse<T>(CODE_FAIL, message, null);
    }

    public static <T> BaseResponse<T> badrequest(T data) {
        return new BaseResponse<T>(CODE_FAIL, "no identifier arguments", data);
    }

    public static <T> BaseResponse<T> badrequest(String message, T data) {
        return new BaseResponse<T>(CODE_FAIL, message, data);
    }

    public static <T> BaseResponse<T> noFound(String message) {
        return new BaseResponse<T>(CODE_NOT_FOUND, message, null);
    }

}
