package com.ezchat.mappers;

import org.apache.ibatis.annotations.Param;

/**
 * @Description:Mapper
 * @author:xiuyuan
 * @date:2024/12/16
 */
public interface GroupInfoMapper<T, P> extends BaseMapper {
	/**
	 * 根据GroupId查询数据
	 */
	T selectByGroupId(@Param("groupId") String groupId);

	/**
	 * 根据GroupId更新数据
	 */
	Integer updateByGroupId(@Param("bean") T t, @Param("groupId") String groupId);

	/**
	 * 根据GroupId删除数据
	 */
	Integer deleteByGroupId(@Param("groupId") String groupId);


}