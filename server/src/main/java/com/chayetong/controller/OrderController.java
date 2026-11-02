package com.chayetong.controller;

import com.chayetong.common.PageResult;
import com.chayetong.common.Result;
import com.chayetong.controller.dto.OrderRequest;
import com.chayetong.entity.PurchaseOrder;
import com.chayetong.security.LoginUser;
import com.chayetong.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 收购单接口：开单/查详情登录即可，改单删单需管理员（由 Security 统一控制）。
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /** 开单：服务端计算净重、金额、单号、开单人 */
    @PostMapping
    public Result<PurchaseOrder> create(@Valid @RequestBody OrderRequest req,
                                        @AuthenticationPrincipal LoginUser loginUser) {
        return Result.ok(orderService.create(req, loginUser));
    }

    /** 收购流水分页查询 */
    @GetMapping
    public Result<PageResult<PurchaseOrder>> page(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String payStatus,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(orderService.page(dateFrom, dateTo, keyword, payStatus, page, size));
    }

    /** 单据详情 */
    @GetMapping("/{id}")
    public Result<PurchaseOrder> get(@PathVariable Long id) {
        return Result.ok(orderService.get(id));
    }

    /** 改单（ADMIN） */
    @PutMapping("/{id}")
    public Result<PurchaseOrder> update(@PathVariable Long id, @Valid @RequestBody OrderRequest req) {
        return Result.ok(orderService.update(id, req));
    }

    /** 删单（ADMIN） */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        orderService.delete(id);
        return Result.ok();
    }
}
