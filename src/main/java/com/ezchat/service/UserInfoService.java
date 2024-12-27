package com.ezchat.service;

import com.ezchat.entity.dto.TokenUserInfoDTO;
import com.ezchat.entity.vo.PaginationResultVO;
import com.ezchat.entity.po.UserInfo;
import com.ezchat.entity.query.UserInfoQuery;
import com.ezchat.entity.vo.UserInfoVo;
import com.ezchat.exception.BusinessException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @Description:用户信息Service
 * @author:xiuyuan
 * @date:2024/12/11
 */
public interface UserInfoService {

	/**
	 * 根据条件查询列表
	 */
	List<UserInfo> findListByParam(UserInfoQuery query);

	/**
	 * 根据条件查询数量
	 */
	Integer findCountByParam(UserInfoQuery query);

	/**
	 * 分页查询
	 */
	PaginationResultVO<UserInfo> findListByPage(UserInfoQuery query);

	/**
	 * 新增
	 */
	Integer add(UserInfo bean);

	/**
	 * 批量新增
	 */
	Integer addBatch(List<UserInfo> listBean);

	/**
	 * 批量新增或更新
	 */
	Integer addOrUpdateBatch(List<UserInfo> listBean);

	/**
	 * 根据UserId查询数据
	 */
	UserInfo getUserInfoByUserId(String userId);

	/**
	 * 根据UserId更新数据
	 */
	Integer updateUserInfoByUserId(UserInfo bean, String userId);

	/**
	 * 根据UserId删除数据
	 */
	Integer deleteUserInfoByUserId(String userId);

	/**
	 * 根据Email查询数据
	 */
	UserInfo getUserInfoByEmail(String email);

	/**
	 * 根据Email更新数据
	 */
	Integer updateUserInfoByEmail(UserInfo bean, String email);

	/**
	 * 根据Email删除数据
	 */
	Integer deleteUserInfoByEmail(String email);

	/**
	 * 注册
	 * @param email
	 * @param nickname
	 * @param password
	 */
	void register (String email, String nickname, String password) throws BusinessException;

	/**
	 * 登录
	 * @param email
	 * @param password
	 */
	UserInfoVo login(String email, String password) throws BusinessException;

	/**
	 * 更新用户信息
	 * @param userInfo
	 * @param avatarFile
	 * @param avatarCover
	 */
    void updateUserInfo(UserInfo userInfo, MultipartFile avatarFile, MultipartFile avatarCover) throws IOException;

	/**
	 * 管理员更新用户状态
	 * @param status
	 * @param userId
	 */
	void updateUserStatus(Integer status, String userId) throws BusinessException;

	/**
	 * 管理员强制用户下线
	 * @param userId
	 */
	void forceOffLine(String userId);

}
