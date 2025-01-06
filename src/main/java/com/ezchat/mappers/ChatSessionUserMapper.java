package com.ezchat.mappers;

import org.apache.ibatis.annotations.Param;

/**
 * @Description:会话用户Mapper
 * @author:xiuyuan
 * @date:2025/01/06
 */
public interface ChatSessionUserMapper<T, P> extends BaseMapper {
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
	 * 根据限定条件query更新数据
	 */
	Integer updateByQuery(@Param("bean")T t, @Param("query") P p);

	/**
	 * 根据限定条件query删除数据
	 */
	Integer deleteByQuery(@Param("bean")T t, @Param("query") P p);
}