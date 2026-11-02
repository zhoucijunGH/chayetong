package com.chayetong.controller.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 品类维护请求。
 */
@Data
public class CategoryRequest {

    @NotBlank(message = "请填写品类名称")
    private String name;

    private String remark;

    /** 1 启用 0 停用 */
    private Integer enabled;
}
