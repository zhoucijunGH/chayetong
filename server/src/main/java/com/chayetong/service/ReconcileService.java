package com.chayetong.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chayetong.common.BizException;
import com.chayetong.entity.Customer;
import com.chayetong.entity.Payment;
import com.chayetong.entity.PurchaseOrder;
import com.chayetong.mapper.CustomerMapper;
import com.chayetong.mapper.PaymentMapper;
import com.chayetong.mapper.PurchaseOrderMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 结算对账服务：客户 + 期间 → 期初应付 / 笔数 / 总重 / 总额 / 已付 / 期末应付 + 逐笔明细。
 *
 * 口径：计入对账 = 已确认单据；
 * 期末应付 = 期初应付 + 期间已确认金额 − 期间付款合计。
 */
@Service
public class ReconcileService {

    private final PurchaseOrderMapper orderMapper;
    private final PaymentMapper paymentMapper;
    private final CustomerMapper customerMapper;

    public ReconcileService(PurchaseOrderMapper orderMapper, PaymentMapper paymentMapper,
                            CustomerMapper customerMapper) {
        this.orderMapper = orderMapper;
        this.paymentMapper = paymentMapper;
        this.customerMapper = customerMapper;
    }

    public Map<String, Object> statement(Long customerId, LocalDate dateFrom, LocalDate dateTo) {
        if (customerId == null) {
            throw new BizException(400, "请选择客户");
        }
        Customer customer = customerMapper.selectById(customerId);
        if (customer == null) {
            throw new BizException(404, "客户不存在");
        }
        LocalDate from = dateFrom != null ? dateFrom : LocalDate.of(1970, 1, 1);
        LocalDate to = dateTo != null ? dateTo : LocalDate.now();

        // 期初：期间之前已确认金额 − 期间之前有效付款
        BigDecimal openingAmount = sumConfirmed(customerId, null, from.minusDays(1));
        BigDecimal openingPaid = sumPaid(customerId, null, from.minusDays(1));
        BigDecimal opening = openingAmount.subtract(openingPaid);

        // 期间明细（已确认）
        List<PurchaseOrder> confirmed = orderMapper.selectList(new LambdaQueryWrapper<PurchaseOrder>()
                .eq(PurchaseOrder::getCustomerId, customerId)
                .eq(PurchaseOrder::getStatus, PurchaseOrder.CONFIRMED)
                .ge(PurchaseOrder::getOrderDate, from)
                .le(PurchaseOrder::getOrderDate, to)
                .orderByAsc(PurchaseOrder::getOrderDate)
                .orderByAsc(PurchaseOrder::getId));

        // 期间未复核（提示用，不计入）
        Long pendingCount = orderMapper.selectCount(new LambdaQueryWrapper<PurchaseOrder>()
                .eq(PurchaseOrder::getCustomerId, customerId)
                .eq(PurchaseOrder::getStatus, PurchaseOrder.PENDING)
                .ge(PurchaseOrder::getOrderDate, from)
                .le(PurchaseOrder::getOrderDate, to));

        // 期间作废（提示用）
        Long voidCount = orderMapper.selectCount(new LambdaQueryWrapper<PurchaseOrder>()
                .eq(PurchaseOrder::getCustomerId, customerId)
                .eq(PurchaseOrder::getStatus, PurchaseOrder.VOID)
                .ge(PurchaseOrder::getOrderDate, from)
                .le(PurchaseOrder::getOrderDate, to));

        BigDecimal periodAmount = BigDecimal.ZERO;
        BigDecimal periodWeight = BigDecimal.ZERO;
        for (PurchaseOrder o : confirmed) {
            periodAmount = periodAmount.add(nvl(o.getAmount()));
            periodWeight = periodWeight.add(nvl(o.getNetWeight()));
        }

        List<Payment> payments = paymentMapper.selectList(new LambdaQueryWrapper<Payment>()
                .eq(Payment::getCustomerId, customerId)
                .eq(Payment::getStatus, "REGISTERED")
                .ge(Payment::getPayDate, from)
                .le(Payment::getPayDate, to)
                .orderByAsc(Payment::getPayDate)
                .orderByAsc(Payment::getId));
        BigDecimal periodPaid = BigDecimal.ZERO;
        for (Payment p : payments) {
            periodPaid = periodPaid.add(nvl(p.getAmount()));
        }

        BigDecimal closing = opening.add(periodAmount).subtract(periodPaid);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("customerId", customer.getId());
        data.put("customerName", customer.getName());
        data.put("customerType", customer.getType());
        data.put("phone", customer.getPhone());
        data.put("dateFrom", dateFrom == null ? null : dateFrom.toString());
        data.put("dateTo", to.toString());
        data.put("opening", opening.setScale(2, RoundingMode.HALF_UP));
        data.put("count", (long) confirmed.size());
        data.put("totalWeight", periodWeight.setScale(2, RoundingMode.HALF_UP));
        data.put("totalAmount", periodAmount.setScale(2, RoundingMode.HALF_UP));
        data.put("paid", periodPaid.setScale(2, RoundingMode.HALF_UP));
        data.put("closing", closing.setScale(2, RoundingMode.HALF_UP));
        data.put("pendingCount", pendingCount);
        data.put("voidCount", voidCount);
        data.put("details", confirmed);
        data.put("payments", payments);
        return data;
    }

    /** 客户余额列表（欠款管理）：余额 = 已确认总额 − 有效付款合计，按余额降序 */
    public List<Map<String, Object>> balances(String keyword) {
        List<Customer> customers = customerMapper.selectList(new LambdaQueryWrapper<Customer>()
                .like(keyword != null && !keyword.isBlank(), Customer::getName, keyword)
                .orderByAsc(Customer::getId));

        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (Customer c : customers) {
            BigDecimal confirmed = sumConfirmed(c.getId(), null, null);
            BigDecimal paid = sumPaid(c.getId(), null, null);
            if (confirmed.compareTo(BigDecimal.ZERO) == 0 && paid.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("customerId", c.getId());
            m.put("name", c.getName());
            m.put("type", c.getType());
            m.put("phone", c.getPhone());
            m.put("status", c.getStatus());
            m.put("confirmedAmount", confirmed.setScale(2, RoundingMode.HALF_UP));
            m.put("paidAmount", paid.setScale(2, RoundingMode.HALF_UP));
            m.put("balance", confirmed.subtract(paid).setScale(2, RoundingMode.HALF_UP));
            result.add(m);
        }
        result.sort((a, b) -> ((BigDecimal) b.get("balance")).compareTo((BigDecimal) a.get("balance")));
        return result;
    }

    private BigDecimal sumConfirmed(Long customerId, LocalDate from, LocalDate to) {
        List<PurchaseOrder> list = orderMapper.selectList(new LambdaQueryWrapper<PurchaseOrder>()
                .eq(PurchaseOrder::getCustomerId, customerId)
                .eq(PurchaseOrder::getStatus, PurchaseOrder.CONFIRMED)
                .ge(from != null, PurchaseOrder::getOrderDate, from)
                .le(to != null, PurchaseOrder::getOrderDate, to));
        BigDecimal sum = BigDecimal.ZERO;
        for (PurchaseOrder o : list) {
            sum = sum.add(nvl(o.getAmount()));
        }
        return sum;
    }

    private BigDecimal sumPaid(Long customerId, LocalDate from, LocalDate to) {
        List<Payment> list = paymentMapper.selectList(new LambdaQueryWrapper<Payment>()
                .eq(Payment::getCustomerId, customerId)
                .eq(Payment::getStatus, "REGISTERED")
                .ge(from != null, Payment::getPayDate, from)
                .le(to != null, Payment::getPayDate, to));
        BigDecimal sum = BigDecimal.ZERO;
        for (Payment p : list) {
            sum = sum.add(nvl(p.getAmount()));
        }
        return sum;
    }

    private BigDecimal nvl(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
