package com.atguigu.gulimall.product.dao;

import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品三级分类
 * 
 * @author hh
 * @email 55333@qq.com
 * @date 2022-06-20 22:08:40
 */
@Mapper
public interface CategoryDao extends BaseMapper<CategoryEntity> {
	
}
