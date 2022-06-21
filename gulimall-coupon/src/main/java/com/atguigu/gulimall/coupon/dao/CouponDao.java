package com.atguigu.gulimall.coupon.dao;

import com.atguigu.gulimall.coupon.entity.CouponEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 优惠券信息
 * 
 * @author hh
 * @email 55333@qq.com
 * @date 2022-06-20 02:40:43
 */
@Mapper
public interface CouponDao extends BaseMapper<CouponEntity> {
	
}
