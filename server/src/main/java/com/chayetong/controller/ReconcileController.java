package com.chayetong.controller;

import com.chayetong.common.Result;
import com.chayetong.service.ReconcileService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 结算对账 / 欠款余额接口：对账需财务或老板（由 Security 统一控制）。
 */
@RestController
@RequestMapping("/api/reconcile")
public class ReconcileController {

    private final ReconcileService reconcileService;

    public ReconcileController(ReconcileService reconcileService) {
        this.reconcileService = reconcileService;
    }

    /** 客户 + 期间对账单：期初/笔数/总重/总额/已付/期末应付 + 逐笔明细 + 付款明细 */
    @GetMapping("/statement")
    public Result<Map<String, Object>> statement(
            @RequestParam Long customerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        return Result.ok(reconcileService.statement(customerId, dateFrom, dateTo));
    }

    /** 欠款余额列表（按余额降序） */
    @GetMapping("/balances")
    public Result<List<Map<String, Object>>> balances(@RequestParam(required = false) String keyword) {
        return Result.ok(reconcileService.balances(keyword));
    }
}
