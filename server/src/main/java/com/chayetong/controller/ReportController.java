package com.chayetong.controller;

import com.chayetong.common.BizException;
import com.chayetong.common.Result;
import com.chayetong.service.ReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

/**
 * 日结报表接口。
 */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    /** 指定日期的日结：汇总 / 按品类 / 按果农 */
    @GetMapping("/daily")
    public Result<Map<String, Object>> daily(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        if (date == null) {
            date = LocalDate.now();
        }
        if (date.isAfter(LocalDate.now())) {
            throw new BizException(400, "不能查询未来日期");
        }
        return Result.ok(reportService.daily(date));
    }
}
