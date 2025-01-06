package com.ezchat.mappers;

import org.apache.ibatis.annotations.Param;

/**
 * @Description:会话信息Mapper
 * @author:xiuyuan
 * @date:2025/01/06
 */
public interface ChatSessionMapper<T, P> extends BaseMapper {
	/**
	 * 根据SessionId查询数据
	 */
	T selectBySessionId(@Param("sessionId") String sessionId);

	/**
	 * 根据SessionId更新数据
	 */
	Integer updateBySessionId(@Param("bean") T t, @Param("sessionId") String sessionId);

	/**
	 * 根据SessionId删除数据
	 */
	Integer deleteBySessionId(@Param("sessionId") String sessionId);



	/**
	 * 根据限定条件query更新数据
	 */
	Integer updateByQuery(@Param("bean")T t, @Param("query") P p);

	/**
	 * 根据限定条件query删除数据
	 */
	Integer deleteByQuery(@Param("bean")T t, @Param("query") P p);
}