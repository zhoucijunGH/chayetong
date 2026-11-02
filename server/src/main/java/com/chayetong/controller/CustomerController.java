package com.chayetong.controller;

import com.chayetong.common.Result;
import com.chayetong.controller.dto.CustomerRequest;
import com.chayetong.entity.Customer;
import com.chayetong.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 供货客户档案接口：查询登录即可，维护仅管理员。
 */
@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    public Result<List<Customer>> list(@RequestParam(required = false) String keyword,
                                       @RequestParam(required = false) Integer status) {
        return Result.ok(customerService.list(keyword, status));
    }

    @PostMapping
    public Result<Customer> create(@Valid @RequestBody CustomerRequest req) {
        return Result.ok(customerService.create(req));
    }

    @PutMapping("/{id}")
    public Result<Customer> update(@PathVariable Long id, @Valid @RequestBody CustomerRequest req) {
        return Result.ok(customerService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        customerService.delete(id);
        return Result.ok();
    }
}
