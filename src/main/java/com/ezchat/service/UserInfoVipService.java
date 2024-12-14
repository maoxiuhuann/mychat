package com.ezchat.service;

import com.ezchat.entity.vo.PaginationResultVO;
import com.ezchat.entity.po.UserInfoVip;
import com.ezchat.entity.query.UserInfoVipQuery;

import java.util.List;
/**
 * @Description:靓号表Service
 * @author:xiuyuan
 * @date:2024/12/11
 */
public interface UserInfoVipService {

	/**
	 * 根据条件查询列表
	 */
	List<UserInfoVip> findListByParam(UserInfoVipQuery query);

	/**
	 * 根据条件查询数量
	 */
	Integer findCountByParam(UserInfoVipQuery query);

	/**
	 * 分页查询
	 */
	PaginationResultVO<UserInfoVip> findListByPage(UserInfoVipQuery query);

	/**
	 * 新增
	 */
	Integer add(UserInfoVip bean);

	/**
	 * 批量新增
	 */
	Integer addBatch(List<UserInfoVip> listBean);

	/**
	 * 批量新增或更新
	 */
	Integer addOrUpdateBatch(List<UserInfoVip> listBean);

	/**
	 * 根据IdAndUserId查询数据
	 */
	UserInfoVip getUserInfoVipByIdAndUserId(Integer id, String userId);

	/**
	 * 根据IdAndUserId更新数据
	 */
	Integer updateUserInfoVipByIdAndUserId(UserInfoVip bean, Integer id, String userId);

	/**
	 * 根据IdAndUserId删除数据
	 */
	Integer deleteUserInfoVipByIdAndUserId(Integer id, String userId);

	/**
	 * 根据UserId查询数据
	 */
	UserInfoVip getUserInfoVipByUserId(String userId);

	/**
	 * 根据UserId更新数据
	 */
	Integer updateUserInfoVipByUserId(UserInfoVip bean, String userId);

	/**
	 * 根据UserId删除数据
	 */
	Integer deleteUserInfoVipByUserId(String userId);

	/**
	 * 根据Email查询数据
	 */
	UserInfoVip getUserInfoVipByEmail(String email);

	/**
	 * 根据Email更新数据
	 */
	Integer updateUserInfoVipByEmail(UserInfoVip bean, String email);

	/**
	 * 根据Email删除数据
	 */
	Integer deleteUserInfoVipByEmail(String email);

}
