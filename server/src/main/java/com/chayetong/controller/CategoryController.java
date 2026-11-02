package com.chayetong.controller;

import com.chayetong.common.Result;
import com.chayetong.controller.dto.CategoryRequest;
import com.chayetong.entity.Category;
import com.chayetong.service.CategoryService;
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
 * 品类档案接口：查询登录即可，维护仅老板/管理员（由 Security 统一控制）。
 */
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public Result<List<Category>> list(@RequestParam(required = false) String keyword,
                                       @RequestParam(required = false) Integer enabled) {
        return Result.ok(categoryService.list(keyword, enabled));
    }

    @PostMapping
    public Result<Category> create(@Valid @RequestBody CategoryRequest req) {
        return Result.ok(categoryService.create(req));
    }

    @PutMapping("/{id}")
    public Result<Category> update(@PathVariable Long id, @Valid @RequestBody CategoryRequest req) {
        return Result.ok(categoryService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return Result.ok();
    }
}
