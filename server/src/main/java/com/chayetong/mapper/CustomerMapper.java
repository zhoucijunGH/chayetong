package com.chayetong.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chayetong.entity.Customer;
import org.apache.ibatis.annotations.Mapper;

/**
 * 供货客户 Mapper。
 */
@Mapper
public interface CustomerMapper extends BaseMapper<Customer> {
}
