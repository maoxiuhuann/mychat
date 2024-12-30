package com.ezchat.mappers;

import org.apache.ibatis.annotations.Param;

/**
 * @Description:app发布Mapper
 * @author:xiuyuan
 * @date:2024/12/30
 */
public interface AppUpdateMapper<T, P> extends BaseMapper {
	/**
	 * 根据Id查询数据
	 */
	T selectById(@Param("id") Integer id);

	/**
	 * 根据Id更新数据
	 */
	Integer updateById(@Param("bean") T t, @Param("id") Integer id);

	/**
	 * 根据Id删除数据
	 */
	Integer deleteById(@Param("id") Integer id);

	/**
	 * 根据限定条件query更新数据
	 */
	Integer updateByQuery(@Param("bean")T t, @Param("query") P p);

	/**
	 * 根据限定条件query删除数据
	 */
	Integer deleteByQuery(@Param("bean")T t, @Param("query") P p);
}