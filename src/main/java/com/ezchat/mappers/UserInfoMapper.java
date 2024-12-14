package com.ezchat.mappers;

import org.apache.ibatis.annotations.Param;

/**
 * @Description:用户信息Mapper
 * @author:xiuyuan
 * @date:2024/12/11
 */
public interface UserInfoMapper<T, P> extends BaseMapper {
	/**
	 * 根据UserId查询数据
	 */
	T selectByUserId(@Param("userId") String userId);

	/**
	 * 根据UserId更新数据
	 */
	Integer updateByUserId(@Param("bean") T t, @Param("userId") String userId);

	/**
	 * 根据UserId删除数据
	 */
	Integer deleteByUserId(@Param("userId") String userId);

	/**
	 * 根据Email查询数据
	 */
	T selectByEmail(@Param("email") String email);

	/**
	 * 根据Email更新数据
	 */
	Integer updateByEmail(@Param("bean") T t, @Param("email") String email);

	/**
	 * 根据Email删除数据
	 */
	Integer deleteByEmail(@Param("email") String email);


}