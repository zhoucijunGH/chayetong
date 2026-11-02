package com.chayetong.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chayetong.entity.Payment;
import org.apache.ibatis.annotations.Mapper;

/**
 * 付款记录 Mapper。
 */
@Mapper
public interface PaymentMapper extends BaseMapper<Payment> {
}
