package com.chayetong.controller;

import com.chayetong.common.PageResult;
import com.chayetong.common.Result;
import com.chayetong.controller.dto.PaymentRequest;
import com.chayetong.controller.dto.VoidRequest;
import com.chayetong.entity.Payment;
import com.chayetong.security.LoginUser;
import com.chayetong.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 付款记录接口：登记/作废需财务或老板（由 Security 统一控制）。
 */
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping
    public Result<PageResult<Payment>> page(
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(paymentService.page(customerId, dateFrom, dateTo, page, size));
    }

    @PostMapping
    public Result<Payment> create(@Valid @RequestBody PaymentRequest req,
                                  @AuthenticationPrincipal LoginUser loginUser) {
        return Result.ok(paymentService.create(req, loginUser));
    }

    /** 付款记录不可修改，只能作废重录；作废后欠款余额回滚 */
    @PostMapping("/{id}/void")
    public Result<Payment> voidPayment(@PathVariable Long id,
                                       @Valid @RequestBody VoidRequest req) {
        return Result.ok(paymentService.voidPayment(id, req.getReason()));
    }
}
