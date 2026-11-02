package com.chayetong.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chayetong.common.BizException;
import com.chayetong.controller.dto.CustomerRequest;
import com.chayetong.entity.Customer;
import com.chayetong.entity.Payment;
import com.chayetong.entity.PurchaseOrder;
import com.chayetong.mapper.CustomerMapper;
import com.chayetong.mapper.PaymentMapper;
import com.chayetong.mapper.PurchaseOrderMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 供货客户服务：查询 / 维护（维护仅管理员）。
 */
@Service
public class CustomerService {

    private final CustomerMapper customerMapper;
    private final PurchaseOrderMapper orderMapper;
    private final PaymentMapper paymentMapper;

    public CustomerService(CustomerMapper customerMapper, PurchaseOrderMapper orderMapper,
                           PaymentMapper paymentMapper) {
        this.customerMapper = customerMapper;
        this.orderMapper = orderMapper;
        this.paymentMapper = paymentMapper;
    }

    /** 客户列表：名称/电话模糊 + 状态过滤 */
    public List<Customer> list(String keyword, Integer status) {
        return customerMapper.selectList(new LambdaQueryWrapper<Customer>()
                .eq(status != null, Customer::getStatus, status)
                .and(StringUtils.hasText(keyword), w -> w
                        .like(Customer::getName, keyword)
                        .or().like(Customer::getPhone, keyword))
                .orderByDesc(Customer::getId));
    }

    public Customer create(CustomerRequest req) {
        Customer c = new Customer();
        apply(c, req);
        c.setStatus(req.getStatus() == null ? 1 : req.getStatus());
        customerMapper.insert(c);
        return c;
    }

    public Customer update(Long id, CustomerRequest req) {
        Customer c = get(id);
        apply(c, req);
        if (req.getStatus() != null) {
            c.setStatus(req.getStatus());
        }
        customerMapper.updateById(c);
        return c;
    }

    /** 删除：有历史单据或付款记录时禁止（只能停用） */
    @Transactional
    public void delete(Long id) {
        get(id);
        Long orders = orderMapper.selectCount(new LambdaQueryWrapper<PurchaseOrder>()
                .eq(PurchaseOrder::getCustomerId, id));
        if (orders != null && orders > 0) {
            throw new BizException(400, "该客户已有收购单，请改为停用");
        }
        Long pays = paymentMapper.selectCount(new LambdaQueryWrapper<Payment>()
                .eq(Payment::getCustomerId, id));
        if (pays != null && pays > 0) {
            throw new BizException(400, "该客户已有付款记录，请改为停用");
        }
        customerMapper.deleteById(id);
    }

    public Customer get(Long id) {
        Customer c = customerMapper.selectById(id);
        if (c == null) {
            throw new BizException(404, "客户不存在");
        }
        return c;
    }

    private void apply(Customer c, CustomerRequest req) {
        c.setName(req.getName().trim());
        c.setType(req.getType());
        c.setPhone(req.getPhone());
        c.setAddress(req.getAddress());
        c.setRemark(req.getRemark());
    }
}
