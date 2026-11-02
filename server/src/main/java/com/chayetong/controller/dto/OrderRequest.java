package com.chayetong.controller.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 开单/改单请求。
 */
@Data
public class OrderRequest {

    /** 果品档案 id */
    @NotNull(message = "请选择果品")
    private Long fruitId;

    /** 毛重 */
    @NotNull(message = "请填写毛重")
    @DecimalMin(value = "0", message = "毛重不能为负数")
    private BigDecimal grossWeight;

    /** 皮重 */
    @NotNull(message = "请填写皮重")
    @DecimalMin(value = "0", message = "皮重不能为负数")
    private BigDecimal tareWeight;

    /** 单价 */
    @NotNull(message = "请填写单价")
    @DecimalMin(value = "0", message = "单价不能为负数")
    private BigDecimal price;

    /** 果农姓名 */
    private String farmerName;

    /** 果农电话 */
    private String farmerPhone;

    /** 来源：田头/市场/合作社 */
    private String source;

    /** PAID 已付 / UNPAID 未付 */
    @NotBlank(message = "请选择付款状态")
    private String payStatus;

    /** 付款方式：现金/微信/转账 */
    private String payMethod;

    /** 备注 */
    private String remark;

    /** 磅单照片 */
    private String photoUrl;
}
