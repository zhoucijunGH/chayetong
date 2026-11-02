package com.chayetong.controller.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 品类等级价格维护请求。
 */
@Data
public class GradeRequest {

    @NotNull(message = "请选择品类")
    private Long categoryId;

    @NotBlank(message = "请填写等级")
    private String grade;

    /** 单价（元/公斤） */
    @NotNull(message = "请填写单价")
    @DecimalMin(value = "0", message = "单价不能为负数")
    private BigDecimal price;

    @NotNull(message = "请选择生效日期")
    private LocalDate effectiveDate;

    /** 失效日期，可空表示长期有效 */
    private LocalDate expiryDate;
}
