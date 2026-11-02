package com.chayetong.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chayetong.entity.PurchaseOrder;
import com.chayetong.mapper.PurchaseOrderMapper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 收购流水导出服务：生成 xlsx 文件流（Apache POI）。
 */
@Service
public class ExportService {

    private static final String[] HEADERS = {
            "单号", "日期", "品类", "规格", "毛重", "皮重", "净重", "单价",
            "金额", "果农", "电话", "来源", "付款状态", "付款方式", "开单人"
    };
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final PurchaseOrderMapper orderMapper;

    public ExportService(PurchaseOrderMapper orderMapper) {
        this.orderMapper = orderMapper;
    }

    /**
     * 导出指定日期区间的收购流水为 xlsx 字节流。
     */
    public byte[] exportOrders(LocalDate dateFrom, LocalDate dateTo) throws IOException {
        List<PurchaseOrder> orders = orderMapper.selectList(new LambdaQueryWrapper<PurchaseOrder>()
                .ge(dateFrom != null, PurchaseOrder::getOrderDate, dateFrom)
                .le(dateTo != null, PurchaseOrder::getOrderDate, dateTo)
                .orderByDesc(PurchaseOrder::getOrderDate)
                .orderByDesc(PurchaseOrder::getId));

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("收购流水");

            // 表头样式：加粗居中
            CellStyle headStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headStyle.setFont(font);
            headStyle.setAlignment(HorizontalAlignment.CENTER);

            Row head = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = head.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headStyle);
            }

            int rowIdx = 1;
            for (PurchaseOrder o : orders) {
                Row row = sheet.createRow(rowIdx++);
                int c = 0;
                row.createCell(c++).setCellValue(o.getOrderNo());
                row.createCell(c++).setCellValue(o.getOrderDate() == null ? "" : o.getOrderDate().format(DATE));
                row.createCell(c++).setCellValue(nvl(o.getFruitName()));
                row.createCell(c++).setCellValue(nvl(o.getSpec()));
                row.createCell(c++).setCellValue(num(o.getGrossWeight()));
                row.createCell(c++).setCellValue(num(o.getTareWeight()));
                row.createCell(c++).setCellValue(num(o.getNetWeight()));
                row.createCell(c++).setCellValue(num(o.getPrice()));
                row.createCell(c++).setCellValue(num(o.getAmount()));
                row.createCell(c++).setCellValue(nvl(o.getFarmerName()));
                row.createCell(c++).setCellValue(nvl(o.getFarmerPhone()));
                row.createCell(c++).setCellValue(nvl(o.getSource()));
                row.createCell(c++).setCellValue("PAID".equals(o.getPayStatus()) ? "已付" : "未付");
                row.createCell(c++).setCellValue(nvl(o.getPayMethod()));
                row.createCell(c).setCellValue(nvl(o.getCreatedByName()));
            }

            // 列宽自适应（近似）
            for (int i = 0; i < HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private String nvl(String v) {
        return v == null ? "" : v;
    }

    private double num(BigDecimal v) {
        return v == null ? 0 : v.doubleValue();
    }
}
