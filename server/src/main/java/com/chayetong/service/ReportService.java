package com.chayetong.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chayetong.entity.PurchaseOrder;
import com.chayetong.mapper.PurchaseOrderMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 日结报表服务：按收购日期汇总（汇总 / 按品类 / 按果农）。
 */
@Service
public class ReportService {

    private final PurchaseOrderMapper orderMapper;

    public ReportService(PurchaseOrderMapper orderMapper) {
        this.orderMapper = orderMapper;
    }

    /**
     * 指定日期的日结报表。
     */
    public Map<String, Object> daily(LocalDate date) {
        List<PurchaseOrder> orders = orderMapper.selectList(new LambdaQueryWrapper<PurchaseOrder>()
                .eq(PurchaseOrder::getOrderDate, date)
                .orderByDesc(PurchaseOrder::getId));

        // 汇总
        BigDecimal amount = BigDecimal.ZERO;
        BigDecimal netWeight = BigDecimal.ZERO;
        BigDecimal unpaidAmount = BigDecimal.ZERO;
        for (PurchaseOrder o : orders) {
            amount = amount.add(nvl(o.getAmount()));
            netWeight = netWeight.add(nvl(o.getNetWeight()));
            if ("UNPAID".equals(o.getPayStatus())) {
                unpaidAmount = unpaidAmount.add(nvl(o.getAmount()));
            }
        }
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("amount", amount);
        summary.put("count", (long) orders.size());
        summary.put("netWeight", netWeight);
        summary.put("unpaidAmount", unpaidAmount);

        // 按品类（品类 + 规格分组）
        Map<String, Map<String, Object>> fruitGroups = new LinkedHashMap<>();
        for (PurchaseOrder o : orders) {
            String key = o.getFruitName() + "#" + o.getSpec();
            Map<String, Object> g = fruitGroups.computeIfAbsent(key, k -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("fruitName", o.getFruitName());
                m.put("spec", o.getSpec());
                m.put("count", 0L);
                m.put("netWeight", BigDecimal.ZERO);
                m.put("amount", BigDecimal.ZERO);
                return m;
            });
            g.put("count", (Long) g.get("count") + 1);
            g.put("amount", ((BigDecimal) g.get("amount")).add(nvl(o.getAmount())));
            g.put("netWeight", ((BigDecimal) g.get("netWeight")).add(nvl(o.getNetWeight())));
        }
        List<Map<String, Object>> byFruit = new ArrayList<>(fruitGroups.values());
        byFruit.forEach(g -> g.put("amount", ((BigDecimal) g.get("amount")).setScale(2, RoundingMode.HALF_UP)));

        // 按果农（姓名 + 电话分组）
        Map<String, Map<String, Object>> farmerGroups = new LinkedHashMap<>();
        for (PurchaseOrder o : orders) {
            String key = o.getFarmerName() + "#" + o.getFarmerPhone();
            Map<String, Object> g = farmerGroups.computeIfAbsent(key, k -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("farmerName", o.getFarmerName());
                m.put("phone", o.getFarmerPhone());
                m.put("count", 0L);
                m.put("amount", BigDecimal.ZERO);
                m.put("unpaidAmount", BigDecimal.ZERO);
                return m;
            });
            g.put("count", (Long) g.get("count") + 1);
            g.put("amount", ((BigDecimal) g.get("amount")).add(nvl(o.getAmount())));
            if ("UNPAID".equals(o.getPayStatus())) {
                g.put("unpaidAmount", ((BigDecimal) g.get("unpaidAmount")).add(nvl(o.getAmount())));
            }
        }
        List<Map<String, Object>> byFarmer = new ArrayList<>(farmerGroups.values());
        byFarmer.forEach(g -> {
            g.put("amount", ((BigDecimal) g.get("amount")).setScale(2, RoundingMode.HALF_UP));
            g.put("unpaidAmount", ((BigDecimal) g.get("unpaidAmount")).setScale(2, RoundingMode.HALF_UP));
        });

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("date", date.toString());
        data.put("summary", summary);
        data.put("byFruit", byFruit);
        data.put("byFarmer", byFarmer);
        return data;
    }

    private BigDecimal nvl(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
