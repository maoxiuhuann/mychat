package com.ezchat.controller;

import com.ezchat.annotation.GlobalInterceptor;
import com.ezchat.entity.dto.TokenUserInfoDTO;
import com.ezchat.entity.po.GroupInfo;
import com.ezchat.entity.po.UserContact;
import com.ezchat.entity.query.GroupInfoQuery;
import com.ezchat.entity.query.UserContactQuery;
import com.ezchat.entity.vo.GroupInfoVo;
import com.ezchat.entity.vo.ResponseVo;
import com.ezchat.enums.GroupStatusEnum;
import com.ezchat.enums.MessageTypeEnum;
import com.ezchat.enums.UserContactStatusEnum;
import com.ezchat.exception.BusinessException;
import com.ezchat.mappers.UserContactMapper;
import com.ezchat.service.GroupInfoService;
import com.ezchat.service.UserContactService;
import com.sun.org.apache.bcel.internal.generic.PUSH;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.List;


/**
 * 群组信息
 */
@RestController
@RequestMapping("/group")
@Validated
public class GroupInfoController extends ABaseController {

    @Resource
    private GroupInfoService groupInfoService;

    @Resource
    private UserContactService userContactService;

    /**
     * 保存群组信息-新增或修改
     *
     * @param request
     * @param groupId
     * @param groupName
     * @param groupNotice
     * @param joinType
     * @param avatarFile
     * @param avatarCover
     * @return
     */
    @RequestMapping("/saveGroup")
    @GlobalInterceptor
    public ResponseVo saveGroup(HttpServletRequest request,
                                String groupId,
                                @NotEmpty String groupName,
                                String groupNotice,
                                @NotNull Integer joinType,
                                MultipartFile avatarFile,//头像文件-原图和缩略图
                                MultipartFile avatarCover) throws IOException, BusinessException {

        //从header中获取token-已经使用AOP保证token不为空即用户已经登陆
        TokenUserInfoDTO tokenUserInfoDTO = getTokenUserInfo(request);
        GroupInfo groupInfo = new GroupInfo();
        groupInfo.setGroupId(groupId);
        groupInfo.setGroupName(groupName);
        groupInfo.setGroupNotice(groupNotice);
        groupInfo.setGroupOwnerId(tokenUserInfoDTO.getUserId());
        groupInfo.setJoinType(joinType);
        this.groupInfoService.saveGroup(groupInfo, avatarFile, avatarCover);
        return getSuccessResponseVo(null);
    }


    /**
     * 加载我创建的群组列表
     *
     * @param request
     * @return
     */
    @RequestMapping("/loadMyGroup")
    @GlobalInterceptor
    public ResponseVo loadMyGroup(HttpServletRequest request) {
        //从header中获取token-已经使用AOP保证token不为空即用户已经登陆
        TokenUserInfoDTO tokenUserInfoDTO = getTokenUserInfo(request);
        //构造查询条件groupInfoQuery
        GroupInfoQuery groupInfoQuery = new GroupInfoQuery();
        groupInfoQuery.setGroupOwnerId(tokenUserInfoDTO.getUserId());
        groupInfoQuery.setOrderBy("create_time desc");
        //查询群组列表
        List<GroupInfo> groupInfoList = this.groupInfoService.findListByParam(groupInfoQuery);
        return getSuccessResponseVo(groupInfoList);
    }


    /**
     * 获取通用群聊详情加群成员数量
     *
     * @param request
     * @param groupId
     * @return
     */
    @RequestMapping("/getGroupInfo")
    @GlobalInterceptor
    public ResponseVo getGroupInfo(HttpServletRequest request,
                                   @NotEmpty String groupId) throws BusinessException {
        GroupInfo groupInfo = getGroupDetailCommon(request, groupId);
        UserContactQuery userContactQuery = new UserContactQuery();
        userContactQuery.setContactId(groupId);
        Integer memberCount = this.userContactService.findCountByParam(userContactQuery);
        groupInfo.setMemberCount(memberCount);
        return getSuccessResponseVo(groupInfo);
    }


    /**
     * 聊天窗口内获取群聊详情
     *
     * @param request
     * @param groupId
     * @return
     * @throws BusinessException
     */
    @RequestMapping("/getGroupInfo4Chat")
    @GlobalInterceptor

    public ResponseVo getGroupInfo4Chat(HttpServletRequest request,
                                        @NotEmpty String groupId) throws BusinessException {
        GroupInfo groupInfo = getGroupDetailCommon(request, groupId);
        UserContactQuery userContactQuery = new UserContactQuery();
        userContactQuery.setContactId(groupId);
        //这个接口好像不需要返回用户数量
        Integer memberCount = this.userContactService.findCountByParam(userContactQuery);
        groupInfo.setMemberCount(memberCount);
        //设置关联查询-查询用户的详细信息
        userContactQuery.setQueryUserInfo(true);
        userContactQuery.setOrderBy("create_time desc");
        userContactQuery.setStatus(UserContactStatusEnum.FRIEND.getStatus());
        List<UserContact> userContactList = this.userContactService.findListByParam(userContactQuery);
        GroupInfoVo groupInfoVo = new GroupInfoVo();
        groupInfoVo.setGroupInfo(groupInfo);
        groupInfoVo.setUserContactList(userContactList);
        return getSuccessResponseVo(groupInfoVo);
    }

    /**
     * 获取通用群聊详情
     *
     * @param request
     * @param groupId
     * @return
     * @throws BusinessException
     */
    public GroupInfo getGroupDetailCommon(HttpServletRequest request, String groupId) throws BusinessException {
        //从header中获取token-已经使用AOP保证token不为空即用户已经登陆
        TokenUserInfoDTO tokenUserInfoDTO = getTokenUserInfo(request);
        //限制只有加入了群聊的成员才能查看群组详情
        UserContact userContact = this.userContactService.getUserContactByUserIdAndContactId(tokenUserInfoDTO.getUserId(), groupId);
        if (null == userContact || !UserContactStatusEnum.FRIEND.getStatus().equals(userContact.getStatus())) {
            throw new BusinessException("您没有权限查看该群组信息-请先加入该群组或群聊已经解散");
        }
        GroupInfo groupInfo = this.groupInfoService.getGroupInfoByGroupId(groupId);
        if (null == groupInfo || !GroupStatusEnum.NORMAL.getStatus().equals(groupInfo.getStatus())) {
            throw new BusinessException("该群组不存在或已经被解散");
        }
        return groupInfo;
    }

    /**
     * 操作群组成员
     * @param request
     * @param groupId
     * @param selectContacts
     * @param opType
     * @return
     * @throws BusinessException
     */
    @RequestMapping("/addOrUpdateGroupMember")
    @GlobalInterceptor
    public ResponseVo addOrUpdateGroupMember(HttpServletRequest request,
                                             @NotEmpty String groupId,
                                             @NotEmpty String selectContacts, @NotNull Integer opType) throws BusinessException {

        TokenUserInfoDTO tokenUserInfoDTO = getTokenUserInfo(request);
        groupInfoService.addOrUpdateGroupMember(tokenUserInfoDTO, groupId, selectContacts, opType);
        return getSuccessResponseVo(null);
    }

    /**
     * 退群
     * @param request
     * @param groupId
     * @return
     * @throws BusinessException
     */
    @RequestMapping("/leaveGroup")
    @GlobalInterceptor
    public ResponseVo leaveGroup(HttpServletRequest request,@NotEmpty String groupId) throws BusinessException {
        TokenUserInfoDTO tokenUserInfoDTO = getTokenUserInfo(request);
        groupInfoService.leaveGroup(tokenUserInfoDTO.getUserId(), groupId, MessageTypeEnum.LEAVE_GROUP);
        return getSuccessResponseVo(null);
    }

    /**
     * 解散群组
     * @param request
     * @param groupId
     * @return
     * @throws BusinessException
     */
    @RequestMapping("/dissolutionGroup")
    @GlobalInterceptor
    public ResponseVo dissolutionGroup(HttpServletRequest request,@NotEmpty String groupId) throws BusinessException {
        TokenUserInfoDTO tokenUserInfoDTO = getTokenUserInfo(request);
        groupInfoService.dissolutionGroup(tokenUserInfoDTO.getUserId(), groupId);
        return getSuccessResponseVo(null);
    }
}
