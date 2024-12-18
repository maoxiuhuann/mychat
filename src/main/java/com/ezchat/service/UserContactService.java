package com.ezchat.service;

import com.ezchat.entity.vo.PaginationResultVO;
import com.ezchat.entity.po.UserContact;
import com.ezchat.entity.query.UserContactQuery;

import java.util.List;
/**
 * @Description:联系人Service
 * @author:xiuyuan
 * @date:2024/12/16
 */
public interface UserContactService {

	/**
	 * 根据条件查询列表
	 */
	List<UserContact> findListByParam(UserContactQuery query);

	/**
	 * 根据条件查询数量
	 */
	Integer findCountByParam(UserContactQuery query);

	/**
	 * 分页查询
	 */
	PaginationResultVO<UserContact> findListByPage(UserContactQuery query);

	/**
	 * 新增
	 */
	Integer add(UserContact bean);

	/**
	 * 批量新增
	 */
	Integer addBatch(List<UserContact> listBean);

	/**
	 * 批量新增或更新
	 */
	Integer addOrUpdateBatch(List<UserContact> listBean);

	/**
	 * 根据UserIdAndContactId查询数据
	 */
	UserContact getUserContactByUserIdAndContactId(String userId, String contactId);

	/**
	 * 根据UserIdAndContactId更新数据
	 */
	Integer updateUserContactByUserIdAndContactId(UserContact bean, String userId, String contactId);

	/**
	 * 根据UserIdAndContactId删除数据
	 */
	Integer deleteUserContactByUserIdAndContactId(String userId, String contactId);

}