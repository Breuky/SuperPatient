package com.superpatient.pojo;

import lombok.Data;

/**
 * 统一 API 响应结果
 */
@Data
public class Result {

    /** 状态码: 1=成功, 0=失败 */
    private Integer code;

    /** 提示信息 */
    private String msg;

    /** 响应数据 */
    private Object data;

    /** 成功 (无数据) */
    public static Result success() {
        Result result = new Result();
        result.code = 1;
        result.msg = "success";
        return result;
    }

    /** 成功 (带数据) */
    public static Result success(Object data) {
        Result result = new Result();
        result.code = 1;
        result.msg = "success";
        result.data = data;
        return result;
    }

    /** 失败 */
    public static Result error(String msg) {
        Result result = new Result();
        result.code = 0;
        result.msg = msg;
        return result;
    }
}
