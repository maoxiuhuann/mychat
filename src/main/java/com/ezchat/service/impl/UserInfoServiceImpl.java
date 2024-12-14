package com.ezchat.service.impl;

import com.ezchat.entity.config.AppConfig;
import com.ezchat.entity.dto.TokenUserInfoDTO;
import com.ezchat.entity.po.UserInfoVip;
import com.ezchat.entity.query.SimplePage;
import com.ezchat.entity.query.UserInfoVipQuery;
import com.ezchat.entity.vo.PaginationResultVO;
import com.ezchat.entity.po.UserInfo;
import com.ezchat.entity.query.UserInfoQuery;
import com.ezchat.enums.PageSize;
import com.ezchat.enums.UserContactTypeEnum;
import com.ezchat.enums.UserStatusEnum;
import com.ezchat.enums.VipAccountStatusEnum;
import com.ezchat.exception.BusinessException;
import com.ezchat.mappers.UserInfoMapper;
import com.ezchat.mappers.UserInfoVipMapper;
import com.ezchat.service.UserInfoService;
import com.ezchat.utils.StringUtils;
import jodd.util.ArraysUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Description:用户信息Service
 * @author:xiuyuan
 * @date:2024/12/11
 */
@Service("userInfoService")
public class UserInfoServiceImpl implements UserInfoService {

    @Autowired
    private UserInfoMapper<UserInfo, UserInfoQuery> userInfoMapper;

    @Autowired
    private UserInfoVipMapper<UserInfoVip, UserInfoQuery> userInfoVipMapper;

    @Autowired
    private AppConfig appConfig;

    /**
     * 根据条件查询列表
     */
    public List<UserInfo> findListByParam(UserInfoQuery query) {
        return this.userInfoMapper.selectList(query);
    }

    /**
     * 根据条件查询数量
     */
    public Integer findCountByParam(UserInfoQuery query) {
        return this.userInfoMapper.selectCount(query);
    }

    /**
     * 分页查询
     */
    public PaginationResultVO<UserInfo> findListByPage(UserInfoQuery query) {
        Integer count = this.findCountByParam(query);
        Integer pageSize = query.getPageSize() == null ? PageSize.SIZE15.getSize() : query.getPageSize();
        SimplePage page = new SimplePage(query.getPageNo(), count, pageSize);
        query.setSimplePage(page);
        List<UserInfo> list = this.findListByParam(query);
        PaginationResultVO<UserInfo> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
        return result;
    }

    /**
     * 新增
     */
    public Integer add(UserInfo bean) {
        return this.userInfoMapper.insert(bean);
    }

    /**
     * 批量新增
     */
    public Integer addBatch(List<UserInfo> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.userInfoMapper.insertBatch(listBean);
    }

    /**
     * 批量新增或更新
     */
    public Integer addOrUpdateBatch(List<UserInfo> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.userInfoMapper.insertOrUpdateBatch(listBean);
    }

    /**
     * 根据UserId查询数据
     */
    public UserInfo getUserInfoByUserId(String userId) {
        return this.userInfoMapper.selectByUserId(userId);
    }

    /**
     * 根据UserId更新数据
     */
    public Integer updateUserInfoByUserId(UserInfo bean, String userId) {
        return this.userInfoMapper.updateByUserId(bean, userId);
    }

    /**
     * 根据UserId删除数据
     */
    public Integer deleteUserInfoByUserId(String userId) {
        return this.userInfoMapper.deleteByUserId(userId);
    }

    /**
     * 根据Email查询数据
     */
    public UserInfo getUserInfoByEmail(String email) {
        return this.userInfoMapper.selectByEmail(email);
    }

    /**
     * 根据Email更新数据
     */
    public Integer updateUserInfoByEmail(UserInfo bean, String email) {
        return this.userInfoMapper.updateByEmail(bean, email);
    }

    /**
     * 根据Email删除数据
     */
    public Integer deleteUserInfoByEmail(String email) {
        return this.userInfoMapper.deleteByEmail(email);
    }

    /**
     * 注册
     *
     * @param email
     * @param nickname
     * @param password
     */
    @Override
    public void register(String email, String nickname, String password) throws BusinessException {
        UserInfo userInfo = this.userInfoMapper.selectByEmail(email);
        //阻断式处理，不符合条件的直接抛出异常，只处理符合条件的情况
        if(null != userInfo){
            throw new BusinessException("邮箱被注册");
        }
        String userId = StringUtils.getUserId();
        UserInfoVip vipAccount = this.userInfoVipMapper.selectByEmail(email);
        //1.靓号必须存在 2.靓号没有使用 根据这两点判断是否可以使用靓号
        boolean useVipAccount = null != vipAccount && VipAccountStatusEnum.NO_USE.getStatus().equals(vipAccount.getStatus());
        if (useVipAccount) {
            userId = UserContactTypeEnum.USER.getPrefix() + StringUtils.getRandomNumber(11);
        }

        //插入新数据到数据库
        Date currDate = new Date();
        userInfo = new UserInfo();
        userInfo.setUserId(userId);
        userInfo.setEmail(email);
        userInfo.setPassword(StringUtils.encodeMd5(password));
        userInfo.setNickName(nickname);
        userInfo.setCreateTime(currDate);
        userInfo.setStatus(UserStatusEnum.ENABLE.getStatus());
        userInfo.setLastOffTime(currDate.getTime());
        this.userInfoMapper.insert(userInfo);

        //更新靓号的使用状态
        if (useVipAccount) {
            UserInfoVip updateVip = new UserInfoVip();
            updateVip.setStatus(VipAccountStatusEnum.USED.getStatus());
            this.userInfoVipMapper.updateByUserId(updateVip, vipAccount.getUserId());
        }
        //TODO 注册完创建机器人好友-发送迎新消息


        /*修改前做法
        Map<String, Object> result = new HashMap<>();
        UserInfo userInfo = this.userInfoMapper.selectByEmail(email);
        if (null == userInfo) {
            String userId = StringUtils.getUserId();
            UserInfoVip vipAccount = this.userInfoVipMapper.selectByEmail(email);
            //1.靓号必须存在 2.靓号没有使用 根据这两点判断是否可以使用靓号
            Boolean useVipAccount = null != vipAccount && VipAccountStatusEnum.NO_USE.getStatus().equals(vipAccount.getStatus());
            if (useVipAccount) {
                userId = UserContactTypeEnum.USER.getPrefix() + StringUtils.getRandomNumber(11);
            }

            //插入新数据到数据库
            Date currDate = new Date();
            userInfo = new UserInfo();
            userInfo.setUserId(userId);
            userInfo.setEmail(email);
            userInfo.setPassword(StringUtils.encodeMd5(password));
            userInfo.setNickName(nickname);
            userInfo.setCreateTime(currDate);
            userInfo.setStatus(UserStatusEnum.ENABLE.getStatus());
            userInfo.setLastOffTime(currDate.getTime());
            this.userInfoMapper.insert(userInfo);

            //更新靓号的使用状态
            if (useVipAccount) {
                UserInfoVip updateVip = new UserInfoVip();
                updateVip.setStatus(VipAccountStatusEnum.USED.getStatus());
                this.userInfoVipMapper.updateByUserId(updateVip, vipAccount.getUserId());
            }
        } else {
            result.put("success", false);
            result.put("errorMsg", "邮箱已存在");
        }
        return result;
        */
    }

    /**
     * 登录-返回token
     *
     * @param email
     * @param password
     */
    @Override
    public TokenUserInfoDTO login(String email, String password) throws BusinessException {

        Map<String, Object> result = new HashMap<>();
        UserInfo userInfo = this.userInfoMapper.selectByEmail(email);
        //阻断式处理，不符合条件的直接抛出异常，只处理符合条件的情况
        if(null == userInfo || !userInfo.getPassword().equals(password)){
            throw new BusinessException("账号或密码错误");
        }
        if(UserStatusEnum.DISABLE.getStatus().equals(userInfo.getStatus())){
            throw new BusinessException("账号已停用");
        }
        //TODO 查询我的群组、联系人
        TokenUserInfoDTO tokenUserInfoDTO = getTokenUserInfoDTO(userInfo);
        return tokenUserInfoDTO;

        /*修改前代码
        Map<String, Object> result = new HashMap<>();
        UserInfo userInfo = this.userInfoMapper.selectByEmail(email);
        if (null == userInfo) {
            result.put("success", false);
            result.put("errorMsg", "账号不存在");
        } else {
            if (!userInfo.getPassword().equals(password)) {
                result.put("success", false);
                result.put("errorMsg", "密码错误");
            } else {
                if (!userInfo.getStatus().equals(UserStatusEnum.ENABLE.getStatus())) {
                    result.put("success", false);
                    result.put("errorMsg", "账号已停用");
                } else {
                    TokenUserInfoDTO tokenUserInfoDTO = getTokenUserInfoDTO(userInfo);
                    result.put("data", tokenUserInfoDTO);
                    result.put("success", true);
                }
            }
        }
        return result;
        */

    }

    /**
     * 设置登录token
     *
     * @param userInfo
     * @return
     */
    private TokenUserInfoDTO getTokenUserInfoDTO(UserInfo userInfo) {
        TokenUserInfoDTO tokenUserInfoDTO = new TokenUserInfoDTO();
        tokenUserInfoDTO.setUserId(userInfo.getUserId());
        tokenUserInfoDTO.setNickName(userInfo.getNickName());
        String adminEmails = appConfig.getAdminEmails();
        if (StringUtils.isEmpty(adminEmails) && ArraysUtil.contains(adminEmails.split(","), userInfo.getEmail())) {
            tokenUserInfoDTO.setAdmin(true);
        } else {
            tokenUserInfoDTO.setAdmin(false);
        }
        return tokenUserInfoDTO;
    }
}

