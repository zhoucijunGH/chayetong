package com.chayetong.controller;

import com.chayetong.service.ExportService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

/**
 * 导出接口：收购流水 xlsx 文件流。
 */
@RestController
@RequestMapping("/api/export")
public class ExportController {

    private static final String CONTENT_TYPE_XLSX =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final ExportService exportService;

    public ExportController(ExportService exportService) {
        this.exportService = exportService;
    }

    /** 导出指定日期区间收购流水，列：单号/日期/品类/规格/毛重/皮重/净重/单价/金额/果农/电话/来源/付款状态/付款方式/开单人 */
    @GetMapping("/orders")
    public void exportOrders(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            HttpServletResponse response) throws IOException {
        byte[] bytes = exportService.exportOrders(dateFrom, dateTo);
        String filename = URLEncoder.encode("收购流水.xlsx", StandardCharsets.UTF_8).replace("+", "%20");
        response.setContentType(CONTENT_TYPE_XLSX);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "attachment;filename=" + filename);
        response.setContentLength(bytes.length);
        response.getOutputStream().write(bytes);
        response.getOutputStream().flush();
    }
}
