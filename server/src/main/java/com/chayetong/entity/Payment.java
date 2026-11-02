package com.chayetong.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 付款记录（针对供货客户），只可作废、作废后余额回滚。
 */
@Data
@TableName("payment")
public class Payment {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 客户 id */
    private Long customerId;

    /** 客户名称（冗余快照） */
    private String customerName;

    /** 付款金额（元） */
    private BigDecimal amount;

    /** 方式：现金 / 转账 / 其他 */
    private String method;

    /** 经办人 */
    private String handler;

    /** 付款日期 */
    private LocalDate payDate;

    /** 凭证照片 */
    private String photoUrl;

    /** REGISTERED 已登记 / VOID 已作废 */
    private String status;

    /** 作废原因 */
    private String voidReason;

    /** 登记人 id */
    private Long createdBy;

    /** 登记人姓名 */
    private String createdByName;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
