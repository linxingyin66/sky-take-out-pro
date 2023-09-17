package com.sky.mapper;

import org.apache.ibatis.annotations.Mapper;

import java.util.Map;

/**
 * @ClassName UserMapper
 * @Description
 * @Author LXY
 * @Date 2023/9/13 15:49
 **/
@Mapper
public interface UserMapper {

    /**
     * 根据动态条件统计用户数量
     * @param map
     * @return
     */
    Integer countByMap(Map map);
}
