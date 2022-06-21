package com.atguigu.gulimall.member.dao;

import com.atguigu.gulimall.member.entity.MemberEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会员
 * 
 * @author hh
 * @email 55333@qq.com
 * @date 2022-06-20 22:16:04
 */
@Mapper
public interface MemberDao extends BaseMapper<MemberEntity> {
	
}
