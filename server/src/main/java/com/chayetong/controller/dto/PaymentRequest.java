package com.chayetong.controller.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 付款登记请求。
 */
@Data
public class PaymentRequest {

    @NotNull(message = "请选择客户")
    private Long customerId;

    @NotNull(message = "请填写付款金额")
    @DecimalMin(value = "0.01", message = "付款金额必须大于 0")
    private BigDecimal amount;

    /** 现金 / 转账 / 其他 */
    private String method;

    /** 经办人 */
    private String handler;

    /** 付款日期（默认今天） */
    private LocalDate payDate;

    private String photoUrl;
}
