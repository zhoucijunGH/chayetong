package com.chayetong.controller.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 供货客户维护请求。
 */
@Data
public class CustomerRequest {

    @NotBlank(message = "请填写客户名称")
    private String name;

    /** 散户 / 贩子 / 合作社 */
    @NotBlank(message = "请选择客户类型")
    private String type;

    private String phone;

    private String address;

    private String remark;

    /** 1 启用 0 停用 */
    private Integer status;
}
