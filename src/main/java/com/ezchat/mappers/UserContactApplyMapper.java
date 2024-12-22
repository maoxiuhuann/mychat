package com.ezchat.mappers;

import org.apache.ibatis.annotations.Param;

/**
 * @Description:联系人申请Mapper
 * @author:xiuyuan
 * @date:2024/12/16
 */
public interface UserContactApplyMapper<T, P> extends BaseMapper {
	/**
	 * 根据ApplyId查询数据
	 */
	T selectByApplyId(@Param("applyId") Integer applyId);

	/**
	 * 根据ApplyId更新数据
	 */
	Integer updateByApplyId(@Param("bean") T t, @Param("applyId") Integer applyId);

	/**
	 * 根据ApplyId删除数据
	 */
	Integer deleteByApplyId(@Param("applyId") Integer applyId);

	/**
	 * 根据ApplyUserIdAndReceiveUserIdAndContactId查询数据
	 */
	T selectByApplyUserIdAndReceiveUserIdAndContactId(@Param("applyUserId") String applyUserId, @Param("receiveUserId") String receiveUserId, @Param("contactId") String contactId);

	/**
	 * 根据ApplyUserIdAndReceiveUserIdAndContactId更新数据
	 */
	Integer updateByApplyUserIdAndReceiveUserIdAndContactId(@Param("bean") T t, @Param("applyUserId") String applyUserId, @Param("receiveUserId") String receiveUserId, @Param("contactId") String contactId);

	/**
	 * 根据ApplyUserIdAndReceiveUserIdAndContactId删除数据
	 */
	Integer deleteByApplyUserIdAndReceiveUserIdAndContactId(@Param("applyUserId") String applyUserId, @Param("receiveUserId") String receiveUserId, @Param("contactId") String contactId);

	/**
	 * 根据限定条件query更新数据
	 */
	Integer updateByParam(@Param("bean")T t, @Param("query") P p);

	/**
	 * 根据限定条件query删除数据
	 */
	Integer deleteByParam(@Param("bean")T t, @Param("query") P p);
}