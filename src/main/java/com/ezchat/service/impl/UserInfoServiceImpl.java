package com.ezchat.service.impl;

import com.ezchat.constans.Constans;
import com.ezchat.entity.config.AppConfig;
import com.ezchat.entity.dto.TokenUserInfoDTO;
import com.ezchat.entity.po.UserInfoVip;
import com.ezchat.entity.query.SimplePage;
import com.ezchat.entity.query.UserInfoVipQuery;
import com.ezchat.entity.vo.PaginationResultVO;
import com.ezchat.entity.po.UserInfo;
import com.ezchat.entity.query.UserInfoQuery;
import com.ezchat.entity.vo.UserInfoVo;
import com.ezchat.enums.*;
import com.ezchat.exception.BusinessException;
import com.ezchat.mappers.UserInfoMapper;
import com.ezchat.mappers.UserInfoVipMapper;
import com.ezchat.redis.RedisComponent;
import com.ezchat.service.UserInfoService;
import com.ezchat.utils.CopyUtils;
import com.ezchat.utils.StringUtils;
import com.sun.org.apache.bcel.internal.generic.IF_ACMPEQ;
import jodd.util.ArraysUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * @Description:用户信息Service
 * @author:xiuyuan
 * @date:2024/12/11
 */
@Service("userInfoService")
public class UserInfoServiceImpl implements UserInfoService {

    @Resource
    private UserInfoMapper<UserInfo, UserInfoQuery> userInfoMapper;

    @Autowired
    private UserInfoVipMapper<UserInfoVip, UserInfoQuery> userInfoVipMapper;

    @Autowired
    private AppConfig appConfig;

    @Autowired
    public RedisComponent redisComponent;


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
    @Transactional(rollbackFor = Exception.class)
    public void register(String email, String nickname, String password) throws BusinessException {
        UserInfo userInfo = this.userInfoMapper.selectByEmail(email);
        //阻断式处理，不符合条件的直接抛出异常，只处理符合条件的情况
        if (null != userInfo) {
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
        userInfo.setJoinType(JoinTypeEnum.APPLY.getType());
        userInfo.setLastOffTime(currDate.getTime());
        this.userInfoMapper.insert(userInfo);

        //更新靓号的使用状态
        if (useVipAccount) {
            UserInfoVip updateVip = new UserInfoVip();
            updateVip.setStatus(VipAccountStatusEnum.USED.getStatus());
            this.userInfoVipMapper.updateByUserId(updateVip, vipAccount.getUserId());
        }
        //TODO 注册完创建机器人好友-发送迎新消息

    }

    /**
     * 登录-返回用户信息
     *
     * @param email
     * @param password
     */
    @Override
    public UserInfoVo login(String email, String password) throws BusinessException {

        UserInfo userInfo = this.userInfoMapper.selectByEmail(email);
        //用户心跳判断用户是否在其他地方登录
        Long lastHeartBeat = redisComponent.getUserHeartBeat(userInfo.getUserId());
        //阻断式处理，不符合条件的直接抛出异常，只处理符合条件的情况
        if (null == userInfo || !StringUtils.encodeMd5(password).equals(userInfo.getPassword())) {
            throw new BusinessException("账号或密码错误");
        }
        if (UserStatusEnum.DISABLE.getStatus().equals(userInfo.getStatus())) {
            throw new BusinessException("账号已停用");
        }
        if (null != lastHeartBeat) {
            throw new BusinessException("账号已在其他地方登录");
        }
        //TODO 查询我的群组
        // TODO 查询我的联系人
        TokenUserInfoDTO tokenUserInfoDTO = getTokenUserInfoDTO(userInfo);
        //保存登录信息tokenUserInfoDTO到redis
        String token = StringUtils.encodeMd5(tokenUserInfoDTO.getUserId() + StringUtils.getRandomString(Constans.LENGTH_20));
        tokenUserInfoDTO.setToken(token);
        redisComponent.saveTokenUserInfoDTO(tokenUserInfoDTO);
        //将当前用户信息userInfoVo存储到vo对象中返回
        UserInfoVo userInfoVo = CopyUtils.copy(userInfo, UserInfoVo.class);
        userInfoVo.setToken(tokenUserInfoDTO.getToken());
        userInfoVo.setAdmin(tokenUserInfoDTO.getAdmin());

        return userInfoVo;
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

    /**
     * 更新用户信息
     *
     * @param userInfo
     * @param avatarFile
     * @param avatarCover
     * @throws IOException
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserInfo(UserInfo userInfo, MultipartFile avatarFile, MultipartFile avatarCover) throws IOException {
        if (null != avatarFile) {
            String baseFolder = appConfig.getProjectFolder() + Constans.FILE_FOLDER_FILE;
            File targetFileFolder = new File(baseFolder + Constans.FILE_FOLDER_AVATAR_NAME);
            if (!targetFileFolder.exists()) {
                targetFileFolder.mkdirs();
            }
            String filePath = targetFileFolder.getPath() + "/" + userInfo.getUserId() + Constans.IMAGE_SUFFIX;
            avatarFile.transferTo(new File(filePath));
            avatarCover.transferTo(new File(filePath+Constans.COVER_IMAGE_SUFFIX));
        }
        //先查询，再更新，缩短事务提交时间
        UserInfo dbInfo = userInfoMapper.selectByUserId(userInfo.getUserId());

        userInfoMapper.updateByUserId(userInfo, userInfo.getUserId());
        String contactNameUpdate = null;
        if (dbInfo.getNickName().equals(userInfo.getNickName())){
            contactNameUpdate = userInfo.getNickName();
        }
        //TODO 更新会很信息中的昵称信息
    }

    /**
     * 管理员更新用户状态
     * @param status
     * @param userId
     */
    @Override
    public void updateUserStatus(Integer status, String userId) throws BusinessException {
        UserStatusEnum userStatusEnum = UserStatusEnum.getByStatus(status);
        if (userStatusEnum == null){
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        UserInfo userInfo = new UserInfo();
        userInfo.setStatus(status);
        userInfoMapper.updateByUserId(userInfo, userId);
    }

    /**
     * 管理员强制用户下线
     * @param userId
     * @return
     * @throws BusinessException
     */
    @Override
    public void forceOffLine(String userId) {
        //TODO 强制下线，清除登录信息，清除缓存信息
    }
}

