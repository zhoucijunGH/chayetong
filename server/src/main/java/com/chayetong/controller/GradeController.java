package com.chayetong.controller;

import com.chayetong.common.Result;
import com.chayetong.controller.dto.GradeRequest;
import com.chayetong.entity.CategoryGrade;
import com.chayetong.service.GradeService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
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
import java.util.List;

/**
 * 品类等级价格接口：查询登录即可（开单带价需要），维护仅管理员。
 */
@RestController
@RequestMapping("/api/grades")
public class GradeController {

    private final GradeService gradeService;

    public GradeController(GradeService gradeService) {
        this.gradeService = gradeService;
    }

    /** 价格列表：按品类/关键字过滤，状态按日期自动流转 */
    @GetMapping
    public Result<List<CategoryGrade>> list(@RequestParam(required = false) Long categoryId,
                                            @RequestParam(required = false) String keyword) {
        return Result.ok(gradeService.list(categoryId, keyword));
    }

    /** 指定品类 + 收购日期的生效价（开单带价） */
    @GetMapping("/effective")
    public Result<CategoryGrade> effective(
            @RequestParam Long categoryId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return Result.ok(gradeService.effective(categoryId, date == null ? LocalDate.now() : date));
    }

    @PostMapping
    public Result<CategoryGrade> create(@Valid @RequestBody GradeRequest req) {
        return Result.ok(gradeService.create(req));
    }

    @PutMapping("/{id}")
    public Result<CategoryGrade> update(@PathVariable Long id, @Valid @RequestBody GradeRequest req) {
        return Result.ok(gradeService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        gradeService.delete(id);
        return Result.ok();
    }
}
