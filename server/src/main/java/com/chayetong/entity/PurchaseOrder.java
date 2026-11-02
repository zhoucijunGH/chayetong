package com.chayetong.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 收购单（核心表）。
 */
@Data
@TableName("purchase_order")
public class PurchaseOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 单号 CG+yyyyMMdd+3位流水号 */
    private String orderNo;

    /** 果品档案 id */
    private Long fruitId;

    /** 品类（冗余快照） */
    private String fruitName;

    /** 规格（冗余快照） */
    private String spec;

    /** 毛重 */
    private BigDecimal grossWeight;

    /** 皮重 */
    private BigDecimal tareWeight;

    /** 净重 = 毛重 - 皮重 */
    private BigDecimal netWeight;

    /** 成交单价（元） */
    private BigDecimal price;

    /** 金额 = 净重 × 单价 */
    private BigDecimal amount;

    /** 果农姓名 */
    private String farmerName;

    /** 果农电话 */
    private String farmerPhone;

    /** 来源：田头/市场/合作社 */
    private String source;

    /** PAID 已付 / UNPAID 未付 */
    private String payStatus;

    /** 现金/微信/转账 */
    private String payMethod;

    /** 磅单照片 */
    private String photoUrl;

    /** 备注 */
    private String remark;

    /** 开单人 id */
    private Long createdBy;

    /** 开单人姓名 */
    private String createdByName;

    /** 收购日期（日结维度） */
    private LocalDate orderDate;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
