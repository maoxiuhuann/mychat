package com.ezchat.service.impl;

import com.ezchat.constans.Constans;
import com.ezchat.entity.config.AppConfig;
import com.ezchat.entity.dto.SysSettingDTO;
import com.ezchat.entity.po.UserContact;
import com.ezchat.entity.query.SimplePage;
import com.ezchat.entity.query.UserContactQuery;
import com.ezchat.entity.query.UserInfoQuery;
import com.ezchat.entity.vo.PaginationResultVO;
import com.ezchat.entity.po.GroupInfo;
import com.ezchat.entity.query.GroupInfoQuery;
import com.ezchat.enums.*;
import com.ezchat.exception.BusinessException;
import com.ezchat.mappers.GroupInfoMapper;
import com.ezchat.mappers.UserContactMapper;
import com.ezchat.redis.RedisComponent;
import com.ezchat.service.GroupInfoService;
import com.ezchat.utils.StringUtils;
import com.sun.org.apache.bcel.internal.generic.IF_ACMPEQ;
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
 * @Description:Service
 * @author:xiuyuan
 * @date:2024/12/16
 */
@Service("groupInfoService")
public class GroupInfoServiceImpl implements GroupInfoService {

    @Resource
    private GroupInfoMapper<GroupInfo, GroupInfoQuery> groupInfoMapper;

    @Autowired
    private RedisComponent redisComponent;

    @Autowired
    private UserContactMapper<UserContact, UserContactQuery> userContactMapper;

    @Autowired
    private AppConfig appConfig;


    /**
     * 根据条件查询列表
     */
    public List<GroupInfo> findListByParam(GroupInfoQuery query) {
        return this.groupInfoMapper.selectList(query);
    }

    /**
     * 根据条件查询数量
     */
    public Integer findCountByParam(GroupInfoQuery query) {
        return this.groupInfoMapper.selectCount(query);
    }

    /**
     * 分页查询
     */
    public PaginationResultVO<GroupInfo> findListByPage(GroupInfoQuery query) {
        Integer count = this.findCountByParam(query);
        Integer pageSize = query.getPageSize() == null ? PageSize.SIZE15.getSize() : query.getPageSize();
        SimplePage page = new SimplePage(query.getPageNo(), count, pageSize);
        query.setSimplePage(page);
        List<GroupInfo> list = this.findListByParam(query);
        PaginationResultVO<GroupInfo> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
        return result;
    }

    /**
     * 新增
     */
    public Integer add(GroupInfo bean) {
        return this.groupInfoMapper.insert(bean);
    }

    /**
     * 批量新增
     */
    public Integer addBatch(List<GroupInfo> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.groupInfoMapper.insertBatch(listBean);
    }

    /**
     * 批量新增或更新
     */
    public Integer addOrUpdateBatch(List<GroupInfo> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.groupInfoMapper.insertOrUpdateBatch(listBean);
    }

    /**
     * 根据GroupId查询数据
     */
    public GroupInfo getGroupInfoByGroupId(String groupId) {
        return this.groupInfoMapper.selectByGroupId(groupId);
    }

    /**
     * 根据GroupId更新数据
     */
    public Integer updateGroupInfoByGroupId(GroupInfo bean, String groupId) {
        return this.groupInfoMapper.updateByGroupId(bean, groupId);
    }

    /**
     * 根据GroupId删除数据
     */
    public Integer deleteGroupInfoByGroupId(String groupId) {
        return this.groupInfoMapper.deleteByGroupId(groupId);
    }

    /**
     * 保存群组信息-新增或修改
     *
     * @param groupInfo
     * @param avatarFile
     * @param avatarCover
     */
    @Override
    @Transactional
    public void saveGroup(GroupInfo groupInfo, MultipartFile avatarFile, MultipartFile avatarCover) throws IOException, BusinessException {

        Date currentDate = new Date();

        //新增
        if (StringUtils.isEmpty(groupInfo.getGroupId())) {
            GroupInfoQuery query = new GroupInfoQuery();
            query.setGroupOwnerId(groupInfo.getGroupOwnerId());
            //查询群主已经创建的群数量
            Integer count = this.groupInfoMapper.selectCount(query);
            SysSettingDTO sysSettingDTO = redisComponent.getSysSetting();
            if (count >= sysSettingDTO.getMaxGroupCount()) {
                throw new BusinessException("最多只能创建" + sysSettingDTO.getMaxGroupCount() + "个群聊，群组数量已达上限，无法创建新的群组！");
            }

            if (null == avatarFile) {
                throw new BusinessException(ResponseCodeEnum.CODE_600);
            }

            groupInfo.setCreateTime(currentDate);
            groupInfo.setGroupId(StringUtils.getGroupId());
            this.groupInfoMapper.insert(groupInfo);

            //将群组添加为联系人
            UserContact userContact = new UserContact();
            userContact.setStatus(UserContactStatusEnum.FRIEND.getStatus());
            userContact.setContactType(UserContactTypeEnum.GROUP.getType());
            userContact.setContactId(groupInfo.getGroupId());
            userContact.setUserId(groupInfo.getGroupOwnerId());
            userContact.setCreateTime(currentDate);
            userContact.setLastUpdateTime(currentDate);
            this.userContactMapper.insert(userContact);

            //TODO 创建会话
            //TODO 发送欢迎消息
        } else {
            //修改
            GroupInfo dbInfo = this.groupInfoMapper.selectByGroupId(groupInfo.getGroupId());
            //判断是否为群主-非群主无法修改群信息
            if (!dbInfo.getGroupOwnerId().equals(groupInfo.getGroupOwnerId())) {
                throw new BusinessException(ResponseCodeEnum.CODE_600);
            }
            this.groupInfoMapper.updateByGroupId(groupInfo, groupInfo.getGroupId());
            //TODO 更新相关表冗余信息

            //TODO 修改群昵称发送ws消息-实时更新群昵称
        }
        if (null == avatarCover){
            return;
        }
        String baseFloder = appConfig.getProjectFolder() + Constans.FILE_FOLDER_FILE;
        File targetFileFolder = new File(baseFloder + Constans.FILE_FOLDER_AVATAR_NAME);
        if (!targetFileFolder.exists()){
            targetFileFolder.mkdirs();
        }
        //可以根据groupId生成唯一的文件名，所以不存数据库
        String filePath = targetFileFolder.getPath() + "/" + groupInfo.getGroupId() + Constans.IMAGE_SUFFIX;

        avatarFile.transferTo(new File(filePath));
        avatarCover.transferTo(new File(filePath + Constans.COVER_IMAGE_SUFFIX));
    }

    /**
     * 解散群组
     * @param groupOwnerId
     * @param groupId
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void dissolutionGroup(String groupOwnerId, String groupId) throws BusinessException {
        GroupInfo dbInfo = groupInfoMapper.selectByGroupId(groupId);
        //判断是否为群主-非群主无法解散群组
        if (null == dbInfo || !dbInfo.getGroupOwnerId().equals(groupOwnerId)){
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        //删除群组
        GroupInfo updateInfo = new GroupInfo();
        updateInfo.setStatus(GroupStatusEnum.DISSOLUTION.status);
        groupInfoMapper.updateByGroupId(updateInfo, groupId);
        //删除群组联系人
        UserContactQuery userContactQuery = new UserContactQuery();
        //条件
        userContactQuery.setContactId(groupId);
        userContactQuery.setContactType(UserContactTypeEnum.GROUP.getType());
        //更新信息
        UserContact updateUserContact = new UserContact();
        updateUserContact.setStatus(UserContactStatusEnum.DEL.getStatus());

        userContactMapper.updateByParam(updateUserContact, userContactQuery);

        //TODO 移除相关群程序的联系人缓存

        //TODO 1.更新会话信息 2.记录群消息 3.发送解散通知消息
    }
}
