package com.ezchat.service.impl;

import com.ezchat.constans.Constans;
import com.ezchat.entity.dto.MessageSendDTO;

import com.ezchat.entity.dto.TokenUserInfoDTO;
import com.ezchat.entity.po.GroupInfo;
import com.ezchat.entity.po.UserContact;
import com.ezchat.entity.po.UserInfo;
import com.ezchat.entity.query.*;
import com.ezchat.entity.vo.PaginationResultVO;
import com.ezchat.entity.po.UserContactApply;
import com.ezchat.enums.*;
import com.ezchat.exception.BusinessException;
import com.ezchat.mappers.GroupInfoMapper;
import com.ezchat.mappers.UserContactApplyMapper;
import com.ezchat.mappers.UserContactMapper;
import com.ezchat.mappers.UserInfoMapper;

import com.ezchat.service.UserContactApplyService;
import com.ezchat.service.UserContactService;
import com.ezchat.utils.StringTools;
import com.ezchat.webSocket.ChannelContextUtils;
import com.ezchat.webSocket.MessageHandler;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

import java.util.Date;
import java.util.List;

/**
 * @Description:联系人申请Service
 * @author:xiuyuan
 * @date:2024/12/16
 */
@Service("userContactApplyService")
public class UserContactApplyServiceImpl implements UserContactApplyService {

    @Resource
    private UserContactApplyMapper<UserContactApply, UserContactApplyQuery> userContactApplyMapper;

    @Resource
    private UserContactMapper<UserContact, UserContactQuery> userContactMapper;

    @Resource
    private UserContactService  userContactService;

    @Resource
    private ChannelContextUtils channelContextUtils;

    @Resource
    private MessageHandler messageHandler;

    @Resource
    private UserInfoMapper<UserInfo, UserInfoQuery> userInfoMapper;

    @Resource
    private GroupInfoMapper<GroupInfo, GroupInfoQuery> groupInfoMapper;



    /**
     * 根据条件查询列表
     */
    public List<UserContactApply> findListByParam(UserContactApplyQuery query) {
        return this.userContactApplyMapper.selectList(query);
    }

    /**
     * 根据条件查询数量
     */
    public Integer findCountByParam(UserContactApplyQuery query) {
        return this.userContactApplyMapper.selectCount(query);
    }

    /**
     * 分页查询
     */
    public PaginationResultVO<UserContactApply> findListByPage(UserContactApplyQuery query) {
        Integer count = this.findCountByParam(query);
        Integer pageSize = query.getPageSize() == null ? PageSize.SIZE15.getSize() : query.getPageSize();
        SimplePage page = new SimplePage(query.getPageNo(), count, pageSize);
        query.setSimplePage(page);
        List<UserContactApply> list = this.findListByParam(query);
        PaginationResultVO<UserContactApply> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
        return result;
    }

    /**
     * 新增
     */
    public Integer add(UserContactApply bean) {
        return this.userContactApplyMapper.insert(bean);
    }

    /**
     * 批量新增
     */
    public Integer addBatch(List<UserContactApply> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.userContactApplyMapper.insertBatch(listBean);
    }

    /**
     * 批量新增或更新
     */
    public Integer addOrUpdateBatch(List<UserContactApply> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.userContactApplyMapper.insertOrUpdateBatch(listBean);
    }

    /**
     * 根据ApplyId查询数据
     */
    public UserContactApply getUserContactApplyByApplyId(Integer applyId) {
        return this.userContactApplyMapper.selectByApplyId(applyId);
    }

    /**
     * 根据ApplyId更新数据
     */
    public Integer updateUserContactApplyByApplyId(UserContactApply bean, Integer applyId) {
        return this.userContactApplyMapper.updateByApplyId(bean, applyId);
    }

    /**
     * 根据ApplyId删除数据
     */
    public Integer deleteUserContactApplyByApplyId(Integer applyId) {
        return this.userContactApplyMapper.deleteByApplyId(applyId);
    }

    /**
     * 根据ApplyUserIdAndReceiveUserIdAndContactId查询数据
     */
    public UserContactApply getUserContactApplyByApplyUserIdAndReceiveUserIdAndContactId(String applyUserId, String receiveUserId, String contactId) {
        return this.userContactApplyMapper.selectByApplyUserIdAndReceiveUserIdAndContactId(applyUserId, receiveUserId, contactId);
    }

    /**
     * 根据ApplyUserIdAndReceiveUserIdAndContactId更新数据
     */
    public Integer updateUserContactApplyByApplyUserIdAndReceiveUserIdAndContactId(UserContactApply bean, String applyUserId, String receiveUserId, String contactId) {
        return this.userContactApplyMapper.updateByApplyUserIdAndReceiveUserIdAndContactId(bean, applyUserId, receiveUserId, contactId);
    }

    /**
     * 根据ApplyUserIdAndReceiveUserIdAndContactId删除数据
     */
    public Integer deleteUserContactApplyByApplyUserIdAndReceiveUserIdAndContactId(String applyUserId, String receiveUserId, String contactId) {
        return this.userContactApplyMapper.deleteByApplyUserIdAndReceiveUserIdAndContactId(applyUserId, receiveUserId, contactId);
    }

    /**
     * 申请添加联系人
     *
     * @param tokenUserInfoDTO
     * @param contactId
     * @param applyInfo
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer applyAdd(TokenUserInfoDTO tokenUserInfoDTO, String contactId, String applyInfo) throws BusinessException {
        UserContactTypeEnum contactTypeEnum = UserContactTypeEnum.getByPrefix(contactId);
        if (null == contactTypeEnum) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        //发送申请的用户
        String applyUserId = tokenUserInfoDTO.getUserId();
        //默认申请信息
        applyInfo = StringTools.isEmpty(applyInfo) ? String.format(Constans.DEFAULT_APPLY_REASON, tokenUserInfoDTO.getNickName()) : applyInfo;
        Long currentTime = System.currentTimeMillis();
        Integer joinType = null;
        //收到申请的用户
        String receiveUserId = contactId;
        //查询是否已经是好友,如果已经拉黑则无法添加
        UserContact userContact = userContactService.getUserContactByUserIdAndContactId(applyUserId, receiveUserId);
        if (null != userContact && ArrayUtils.contains(new Integer[]{
                UserContactStatusEnum.BLACKLIST_BE.getStatus(),
                UserContactStatusEnum.BLACKLIST_BE_FIRST.getStatus()}, userContact.getStatus())
        ) {
            throw new BusinessException("对方将你拉黑，无法添加");
        }
        if (UserContactTypeEnum.GROUP == contactTypeEnum) {
            GroupInfo groupInfo = this.groupInfoMapper.selectByGroupId(contactId);
            if (null == groupInfo || GroupStatusEnum.DISSOLUTION.getStatus().equals(groupInfo.getStatus())) {
                throw new BusinessException("该群不存在或已解散，无法添加");
            }

            receiveUserId = groupInfo.getGroupOwnerId();
            joinType = groupInfo.getJoinType();
        } else {
            UserInfo userInfo = this.userInfoMapper.selectByUserId(contactId);
            if (null == userInfo) {
                throw new BusinessException(ResponseCodeEnum.CODE_600);
            }
            joinType = userInfo.getJoinType();
        }
        //直接加入不用添加申请记录
        if (JoinTypeEnum.JOIN.getType().equals(joinType)) {
            userContactService.addContact(applyUserId, receiveUserId, contactId, contactTypeEnum.getType(), applyInfo);
            return joinType;
        }
        //查询数据库中是否已经有申请记录
        //todo 下面代码修改过，修改前的只有if-else
        UserContactApply dbApply = this.userContactApplyMapper.selectByApplyUserIdAndReceiveUserIdAndContactId(applyUserId, receiveUserId, contactId);
        if (null == dbApply) {
            UserContactApply apply = new UserContactApply();
            apply.setApplyUserId(applyUserId);
            apply.setContactType(contactTypeEnum.getType());
            apply.setReceiveUserId(receiveUserId);
            apply.setLastApplyTime(currentTime);
            apply.setContactId(contactId);
            apply.setStatus(UserContactApplyStatusEnum.INIT.getStatus());
            apply.setApplyInfo(applyInfo);
            this.userContactApplyMapper.insert(apply);
        } else if (dbApply.getStatus().equals(UserContactApplyStatusEnum.INIT.getStatus()) || dbApply.getStatus().equals(UserContactApplyStatusEnum.REJECT.getStatus())) {
            //如果已经有申请记录，则更新申请信息
            UserContactApply apply = new UserContactApply();
            apply.setStatus(UserContactApplyStatusEnum.INIT.getStatus());
            apply.setLastApplyTime(currentTime);
            apply.setApplyInfo(applyInfo);
            this.userContactApplyMapper.updateByApplyId(apply, dbApply.getApplyId());
        }else if (dbApply.getStatus().equals(UserContactApplyStatusEnum.BLACKLIST.getStatus())){
            throw new BusinessException("对方以将你拉黑，无法添加");
        }else {
            throw new BusinessException("你已是该群的成员，无法重复申请加入");
        }
        // 发送申请信息给接收方
        if (dbApply == null || !UserContactApplyStatusEnum.INIT.getStatus().TYPE.equals(dbApply.getStatus())) {
            MessageSendDTO messageSendDTO = new MessageSendDTO();
            messageSendDTO.setMessageType(MessageTypeEnum.CONTACT_APPLY.getType());
            messageSendDTO.setMessageContent(applyInfo);
            messageSendDTO.setContactId(receiveUserId);
            messageHandler.sendMessage(messageSendDTO);
        }
        return joinType;
    }


    /**
     * 处理申请
     *
     * @param userId
     * @param applyId
     * @param status
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void dealWithApply(String userId, Integer applyId, Integer status) throws BusinessException {
        UserContactApplyStatusEnum statusEnum = UserContactApplyStatusEnum.getByStatus(status);
        // 不处理默认状态的申请
        if (statusEnum == null || UserContactApplyStatusEnum.INIT.equals(statusEnum)) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        // 根据applyId查询申请信息
        UserContactApply applyInfo = this.userContactApplyMapper.selectByApplyId(applyId);
        // 后端校验申请信息是否存在以及是否为当前用户在操作
        if (applyInfo == null || !userId.equals(applyInfo.getReceiveUserId())) {
            throw new BusinessException(ResponseCodeEnum.CODE_601);
        }
        // 更新申请状态：  	update user_contact_apply set `status` = ?,last_off_time = ? where apply_id = ?
        UserContactApply updateInfo = new UserContactApply();
        updateInfo.setStatus(status);
        updateInfo.setLastApplyTime(System.currentTimeMillis());
        //限制只能在原状态下修改状态  	update user_contact_apply set `status` = ?,last_off_time = ? where apply_id = ? and `status` = 0
        //区别就在拼接的where条件中增加了status=0，只能操作未处理的申请
        UserContactApplyQuery query = new UserContactApplyQuery();
        query.setApplyId(applyId);
        query.setStatus(UserContactApplyStatusEnum.INIT.getStatus());
        Integer count = this.userContactApplyMapper.updateByQuery(updateInfo, query);
        if (count == 0) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        // 处理同意申请，双方双向添加好友关系
        if (UserContactApplyStatusEnum.PASS.getStatus().equals(status)) {
            userContactService.addContact(applyInfo.getApplyUserId(), applyInfo.getReceiveUserId(), applyInfo.getContactId(), applyInfo.getContactType(), applyInfo.getApplyInfo());
        }

        if (UserContactApplyStatusEnum.BLACKLIST.getStatus().equals(status)) {
            Date currentDate = new Date();
            UserContact userContact = new UserContact();
            userContact.setUserId(applyInfo.getApplyUserId());
            userContact.setContactId(applyInfo.getContactId());
            userContact.setContactType(applyInfo.getContactType());
            userContact.setCreateTime(currentDate);
            userContact.setStatus(UserContactStatusEnum.BLACKLIST_BE_FIRST.getStatus());
            userContact.setLastUpdateTime(currentDate);
            userContactMapper.insertOrUpdate(userContact);
        }
    }



}
