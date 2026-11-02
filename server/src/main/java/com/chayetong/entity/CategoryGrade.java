package com.chayetong.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 品类等级价格：品类 × 等级 × 生效区间 → 单价（元/公斤）。
 * 状态按日期自动流转：未生效 / 生效中 / 已失效。
 */
@Data
@TableName("category_grade")
public class CategoryGrade {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 品类 id */
    private Long categoryId;

    /** 品类名称（冗余快照） */
    private String categoryName;

    /** 等级，如 特级/一级/统货 */
    private String grade;

    /** 单价（元/公斤） */
    private BigDecimal price;

    /** 生效日期（含当天） */
    private LocalDate effectiveDate;

    /** 失效日期（含当天），可为空表示长期有效 */
    private LocalDate expiryDate;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /** 状态：NOT_STARTED 未生效 / ACTIVE 生效中 / EXPIRED 已失效（按日期自动流转，不落库） */
    @TableField(exist = false)
    private String status;

    /** 计算当前状态 */
    public String computeStatus(LocalDate today) {
        if (effectiveDate != null && effectiveDate.isAfter(today)) {
            return "NOT_STARTED";
        }
        if (expiryDate != null && expiryDate.isBefore(today)) {
            return "EXPIRED";
        }
        return "ACTIVE";
    }
}
