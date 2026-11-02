package com.chayetong.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chayetong.common.BizException;
import com.chayetong.common.PageResult;
import com.chayetong.controller.dto.OrderRequest;
import com.chayetong.entity.Fruit;
import com.chayetong.entity.PurchaseOrder;
import com.chayetong.entity.SysUser;
import com.chayetong.mapper.FruitMapper;
import com.chayetong.mapper.PurchaseOrderMapper;
import com.chayetong.mapper.SysUserMapper;
import com.chayetong.security.LoginUser;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 收购单服务：开单（服务端计算净重/金额/单号）、查询、改单、删单。
 */
@Service
public class OrderService {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final PurchaseOrderMapper orderMapper;
    private final FruitMapper fruitMapper;
    private final SysUserMapper userMapper;

    public OrderService(PurchaseOrderMapper orderMapper, FruitMapper fruitMapper, SysUserMapper userMapper) {
        this.orderMapper = orderMapper;
        this.fruitMapper = fruitMapper;
        this.userMapper = userMapper;
    }

    /**
     * 开单：净重 = 毛重 - 皮重，金额 = 净重 × 单价，
     * 单号 = "CG"+yyyyMMdd+3位当日流水号，收购日期 = 今天。
     */
    public PurchaseOrder create(OrderRequest req, LoginUser loginUser) {
        Fruit fruit = fruitMapper.selectById(req.getFruitId());
        if (fruit == null) {
            throw new BizException(404, "果品不存在");
        }
        if (req.getGrossWeight().subtract(req.getTareWeight()).compareTo(BigDecimal.ZERO) < 0) {
            throw new BizException(400, "皮重不能大于毛重");
        }

        PurchaseOrder order = new PurchaseOrder();
        order.setFruitId(fruit.getId());
        order.setFruitName(fruit.getName());
        order.setSpec(fruit.getSpec());
        order.setGrossWeight(req.getGrossWeight());
        order.setTareWeight(req.getTareWeight());
        order.setPrice(req.getPrice());
        calc(order);
        order.setFarmerName(req.getFarmerName());
        order.setFarmerPhone(req.getFarmerPhone());
        order.setSource(req.getSource());
        order.setPayStatus(req.getPayStatus());
        order.setPayMethod(req.getPayMethod());
        order.setRemark(req.getRemark());
        order.setPhotoUrl(req.getPhotoUrl());
        order.setOrderDate(LocalDate.now());
        order.setOrderNo(nextOrderNo(LocalDate.now()));
        fillCreator(order, loginUser);

        orderMapper.insert(order);
        return order;
    }

    /** 收购流水分页查询：日期区间 + 果农/单号/品类模糊 + 付款状态 */
    public PageResult<PurchaseOrder> page(LocalDate dateFrom, LocalDate dateTo, String keyword,
                                          String payStatus, int page, int size) {
        LambdaQueryWrapper<PurchaseOrder> wrapper = new LambdaQueryWrapper<PurchaseOrder>()
                .ge(dateFrom != null, PurchaseOrder::getOrderDate, dateFrom)
                .le(dateTo != null, PurchaseOrder::getOrderDate, dateTo)
                .eq(StringUtils.hasText(payStatus), PurchaseOrder::getPayStatus, payStatus)
                .and(StringUtils.hasText(keyword), w -> w
                        .like(PurchaseOrder::getFarmerName, keyword)
                        .or().like(PurchaseOrder::getOrderNo, keyword)
                        .or().like(PurchaseOrder::getFruitName, keyword))
                .orderByDesc(PurchaseOrder::getOrderDate)
                .orderByDesc(PurchaseOrder::getId);
        IPage<PurchaseOrder> result = orderMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(result.getTotal(), result.getRecords());
    }

    /** 单据详情 */
    public PurchaseOrder get(Long id) {
        PurchaseOrder order = orderMapper.selectById(id);
        if (order == null) {
            throw new BizException(404, "收购单不存在");
        }
        return order;
    }

    /** 改单（管理员）：允许改重量、单价、果农、付款等，重算净重与金额 */
    public PurchaseOrder update(Long id, OrderRequest req) {
        PurchaseOrder order = get(id);
        Fruit fruit = fruitMapper.selectById(req.getFruitId());
        if (fruit == null) {
            throw new BizException(404, "果品不存在");
        }
        if (req.getGrossWeight().subtract(req.getTareWeight()).compareTo(BigDecimal.ZERO) < 0) {
            throw new BizException(400, "皮重不能大于毛重");
        }
        order.setFruitId(fruit.getId());
        order.setFruitName(fruit.getName());
        order.setSpec(fruit.getSpec());
        order.setGrossWeight(req.getGrossWeight());
        order.setTareWeight(req.getTareWeight());
        order.setPrice(req.getPrice());
        calc(order);
        order.setFarmerName(req.getFarmerName());
        order.setFarmerPhone(req.getFarmerPhone());
        order.setSource(req.getSource());
        order.setPayStatus(req.getPayStatus());
        order.setPayMethod(req.getPayMethod());
        order.setRemark(req.getRemark());
        order.setPhotoUrl(req.getPhotoUrl());
        orderMapper.updateById(order);
        return order;
    }

    /** 删单（管理员） */
    public void delete(Long id) {
        get(id);
        orderMapper.deleteById(id);
    }

    /** 计算净重与金额 */
    private void calc(PurchaseOrder order) {
        order.setNetWeight(order.getGrossWeight().subtract(order.getTareWeight()));
        order.setAmount(order.getNetWeight().multiply(order.getPrice()).setScale(2, java.math.RoundingMode.HALF_UP));
    }

    /** 当日单号：CG + yyyyMMdd + 3 位流水号（如 CG20261021001） */
    private String nextOrderNo(LocalDate date) {
        String prefix = "CG" + date.format(DAY);
        Long count = orderMapper.selectCount(new LambdaQueryWrapper<PurchaseOrder>()
                .likeRight(PurchaseOrder::getOrderNo, prefix));
        return prefix + String.format("%03d", count + 1);
    }

    /** 开单人：取当前登录人，姓名优先用昵称 */
    private void fillCreator(PurchaseOrder order, LoginUser loginUser) {
        order.setCreatedBy(loginUser.getUserId());
        SysUser user = userMapper.selectById(loginUser.getUserId());
        String name = user != null && StringUtils.hasText(user.getNickname())
                ? user.getNickname() : loginUser.getUsername();
        order.setCreatedByName(name);
    }
}
