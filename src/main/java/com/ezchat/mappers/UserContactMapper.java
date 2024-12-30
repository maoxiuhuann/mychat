package com.ezchat.mappers;

import org.apache.ibatis.annotations.Param;

/**
 * @Description:联系人Mapper
 * @author:xiuyuan
 * @date:2024/12/16
 */
public interface UserContactMapper<T, P> extends BaseMapper {
	/**
	 * 根据UserIdAndContactId查询数据
	 */
	T selectByUserIdAndContactId(@Param("userId") String userId, @Param("contactId") String contactId);

	/**
	 * 根据UserIdAndContactId更新数据
	 */
	Integer updateByUserIdAndContactId(@Param("bean") T t, @Param("userId") String userId, @Param("contactId") String contactId);

	/**
	 * 根据UserIdAndContactId删除数据
	 */
	Integer deleteByUserIdAndContactId(@Param("userId") String userId, @Param("contactId") String contactId);

	/**
	 * 多条件更新
	 */
    void updateByParam(@Param("bean")T t, @Param("query") P p);

	/**
	 * 多条件删除
	 */
	void deleteByParam(@Param("bean")T t, @Param("query") P p);
}