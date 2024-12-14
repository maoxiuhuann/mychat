package com.ezchat.mappers;

import org.apache.ibatis.annotations.Param;

/**
 * @Description:靓号表Mapper
 * @author:xiuyuan
 * @date:2024/12/11
 */
public interface UserInfoVipMapper<T, P> extends BaseMapper {
	/**
	 * 根据IdAndUserId查询数据
	 */
	T selectByIdAndUserId(@Param("id") Integer id, @Param("userId") String userId);

	/**
	 * 根据IdAndUserId更新数据
	 */
	Integer updateByIdAndUserId(@Param("bean") T t, @Param("id") Integer id, @Param("userId") String userId);

	/**
	 * 根据IdAndUserId删除数据
	 */
	Integer deleteByIdAndUserId(@Param("id") Integer id, @Param("userId") String userId);

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