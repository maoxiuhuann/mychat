package com.ezchat.mappers;

import org.apache.ibatis.annotations.Param;

/**
 * @Description:聊天消息表Mapper
 * @author:xiuyuan
 * @date:2025/01/06
 */
public interface ChatMessageMapper<T, P> extends BaseMapper {
	/**
	 * 根据MessageId查询数据
	 */
	T selectByMessageId(@Param("messageId") Long messageId);

	/**
	 * 根据MessageId更新数据
	 */
	Integer updateByMessageId(@Param("bean") T t, @Param("messageId") Long messageId);

	/**
	 * 根据MessageId删除数据
	 */
	Integer deleteByMessageId(@Param("messageId") Long messageId);



	/**
	 * 根据限定条件query更新数据
	 */
	Integer updateByQuery(@Param("bean")T t, @Param("query") P p);

	/**
	 * 根据限定条件query删除数据
	 */
	Integer deleteByQuery(@Param("bean")T t, @Param("query") P p);
}