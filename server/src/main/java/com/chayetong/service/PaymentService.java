package com.chayetong.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chayetong.common.BizException;
import com.chayetong.common.PageResult;
import com.chayetong.controller.dto.PaymentRequest;
import com.chayetong.entity.Customer;
import com.chayetong.entity.Payment;
import com.chayetong.entity.SysUser;
import com.chayetong.mapper.CustomerMapper;
import com.chayetong.mapper.PaymentMapper;
import com.chayetong.mapper.SysUserMapper;
import com.chayetong.security.LoginUser;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;

/**
 * 付款登记服务：只可登记与作废（作废后欠款余额回滚），不可修改。
 */
@Service
public class PaymentService {

    private final PaymentMapper paymentMapper;
    private final CustomerMapper customerMapper;
    private final SysUserMapper userMapper;

    public PaymentService(PaymentMapper paymentMapper, CustomerMapper customerMapper,
                          SysUserMapper userMapper) {
        this.paymentMapper = paymentMapper;
        this.customerMapper = customerMapper;
        this.userMapper = userMapper;
    }

    /** 付款记录列表：客户 + 日期区间 */
    public PageResult<Payment> page(Long customerId, LocalDate dateFrom, LocalDate dateTo,
                                    int page, int size) {
        IPage<Payment> result = paymentMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Payment>()
                        .eq(customerId != null, Payment::getCustomerId, customerId)
                        .ge(dateFrom != null, Payment::getPayDate, dateFrom)
                        .le(dateTo != null, Payment::getPayDate, dateTo)
                        .orderByDesc(Payment::getPayDate)
                        .orderByDesc(Payment::getId));
        return PageResult.of(result.getTotal(), result.getRecords());
    }

    /** 登记付款 */
    public Payment create(PaymentRequest req, LoginUser loginUser) {
        Customer customer = customerMapper.selectById(req.getCustomerId());
        if (customer == null) {
            throw new BizException(404, "客户不存在");
        }
        Payment p = new Payment();
        p.setCustomerId(customer.getId());
        p.setCustomerName(customer.getName());
        p.setAmount(req.getAmount());
        p.setMethod(StringUtils.hasText(req.getMethod()) ? req.getMethod() : "现金");
        p.setHandler(req.getHandler());
        p.setPayDate(req.getPayDate() == null ? LocalDate.now() : req.getPayDate());
        p.setPhotoUrl(req.getPhotoUrl());
        p.setStatus("REGISTERED");
        p.setCreatedBy(loginUser.getUserId());
        p.setCreatedByName(currentName(loginUser));
        paymentMapper.insert(p);
        return p;
    }

    /** 作废付款（不可修改，只能作废重录） */
    public Payment voidPayment(Long id, String reason) {
        Payment p = paymentMapper.selectById(id);
        if (p == null) {
            throw new BizException(404, "付款记录不存在");
        }
        if ("VOID".equals(p.getStatus())) {
            throw new BizException(400, "付款记录已作废");
        }
        p.setStatus("VOID");
        p.setVoidReason(reason);
        paymentMapper.updateById(p);
        return p;
    }

    private String currentName(LoginUser loginUser) {
        SysUser user = userMapper.selectById(loginUser.getUserId());
        return user != null && StringUtils.hasText(user.getNickname())
                ? user.getNickname() : loginUser.getUsername();
    }
}
