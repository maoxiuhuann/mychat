package com.ezchat.controller;


import com.ezchat.annotation.GlobalInterceptor;
import com.ezchat.entity.dto.TokenUserInfoDTO;
import com.ezchat.entity.dto.UserContactSearchResultDTO;
import com.ezchat.entity.po.UserContact;
import com.ezchat.entity.po.UserInfo;
import com.ezchat.entity.query.UserContactApplyQuery;
import com.ezchat.entity.query.UserContactQuery;
import com.ezchat.entity.vo.PaginationResultVO;
import com.ezchat.entity.vo.ResponseVo;
import com.ezchat.entity.vo.UserInfoVo;
import com.ezchat.enums.PageSize;
import com.ezchat.enums.ResponseCodeEnum;
import com.ezchat.enums.UserContactStatusEnum;
import com.ezchat.enums.UserContactTypeEnum;
import com.ezchat.exception.BusinessException;
import com.ezchat.service.UserContactApplyService;
import com.ezchat.service.UserContactService;
import com.ezchat.service.UserInfoService;
import com.ezchat.utils.CopyUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Iterator;
import java.util.List;

@RestController
@RequestMapping("/contact")
public class UserContactController extends ABaseController {

    @Resource
    private UserContactService userContactService;

    @Resource
    private UserInfoService userInfoService;

    @Resource
    private UserContactApplyService userContactApplyService;


    /**
     * 搜索联系人
     *
     * @param request
     * @param contactId
     * @return
     */
    @RequestMapping("/search")
    @GlobalInterceptor
    public ResponseVo search(HttpServletRequest request, @NotEmpty String contactId) {
        TokenUserInfoDTO tokenUserInfoDTO = getTokenUserInfo(request);
        UserContactSearchResultDTO resultDTO = userContactService.searchContact(tokenUserInfoDTO.getUserId(), contactId);
        return getSuccessResponseVo(resultDTO);
    }

    /**
     * 申请添加联系人
     *
     * @param request
     * @param contactId
     * @return
     */
    @RequestMapping("/applyAdd")
    @GlobalInterceptor
    public ResponseVo applyAdd(HttpServletRequest request, @NotEmpty String contactId, String applyInfo) throws BusinessException {
        TokenUserInfoDTO tokenUserInfoDTO = getTokenUserInfo(request);
        Integer joinType = userContactService.applyAdd(tokenUserInfoDTO, contactId, applyInfo);
        return getSuccessResponseVo(joinType);
    }

    /**
     * 加载申请列表
     *
     * @param request
     * @param pageNo
     * @return
     */
    @RequestMapping("/loadApply")
    @GlobalInterceptor
    public ResponseVo loadApply(HttpServletRequest request, Integer pageNo) {
        TokenUserInfoDTO tokenUserInfoDTO = getTokenUserInfo(request);
        UserContactApplyQuery query = new UserContactApplyQuery();
        query.setOrderBy("last_apply_time desc");
        query.setReceiveUserId(tokenUserInfoDTO.getUserId());
        query.setPageNo(pageNo);
        query.setPageSize(PageSize.SIZE15.getSize());
        //加载申请人的昵称-用户查看自己的申请列表时，需要显示申请人的昵称
        query.setQueryContactInfo(true);
        //TODO 根据实际sql更新xml文件：例如表取别名及时在相关代码中更新
        //SELECT 查询的sql语句
        //	a.*,
        //CASE
        //
        //		WHEN a.contact_type = 0 THEN
        //		u.nick_name
        //		WHEN a.contact_type = 1 THEN
        //		g.group_name
        //	END AS contactName
        //FROM
        //	user_contact_apply a
        //	LEFT JOIN user_info u ON u.user_id = a.apply_user_id and a.receive_user_id = #{query.receiveUserId}
        //	LEFT JOIN group_info g ON g.group_id = a.contact_id and a.receive_user_id = #{query.receiveUserId}
        //WHERE
        //	receive_user_id = 'U47173810002'
        PaginationResultVO resultVO = userContactApplyService.findListByPage(query);
        return getSuccessResponseVo(resultVO);
    }

    /**
     * 处理申请
     *
     * @param request
     * @param applyId
     * @param status
     * @return
     */
    @RequestMapping("/dealWithApply")
    @GlobalInterceptor
    public ResponseVo dealWithApply(HttpServletRequest request, @NotNull Integer applyId, @NotNull Integer status) throws BusinessException {
        TokenUserInfoDTO tokenUserInfoDTO = getTokenUserInfo(request);
        this.userContactApplyService.dealWithApply(tokenUserInfoDTO.getUserId(), applyId, status);
        return getSuccessResponseVo(null);
    }

    /**
     * 加载联系人列表
     *
     * @param request
     * @param contactType
     * @return
     * @throws BusinessException
     */
    @RequestMapping("/loadContact")
    @GlobalInterceptor
    public ResponseVo loadContact(HttpServletRequest request, @NotNull String contactType) throws BusinessException {
        UserContactTypeEnum contactTypeEnum = UserContactTypeEnum.getByName(contactType);
        if (null == contactTypeEnum) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        TokenUserInfoDTO tokenUserInfoDTO = getTokenUserInfo(request);
        UserContactQuery query = new UserContactQuery();
        query.setUserId(tokenUserInfoDTO.getUserId());
        query.setContactType(contactTypeEnum.getType());
        // todo 如果用户首次向对方发生申请就被拉黑了，则对方不显示在自己的联系人列表中
        if (contactTypeEnum.USER == contactTypeEnum) {
            //加载联系人的昵称-用户查看自己的联系人列表时，需要显示联系人的昵称
            query.setQueryContactUserInfo(true);
        } else if (contactTypeEnum.GROUP == contactTypeEnum) {
            query.setQueryGroupInfo(true);
            //过滤自己创建的群组
            query.setExcludeMyGroup(true);
        }
        query.setOrderBy("last_update_time desc");
        //状态数组-有些状态的联系人需要过滤掉-被好友拉黑能够看到好友，但是不能发消息
        query.setStatusArray(new Integer[]{UserContactStatusEnum.FRIEND.getStatus(), UserContactStatusEnum.DEL_BE.getStatus(), UserContactStatusEnum.BLACKLIST_BE.getStatus()});
        List<UserContact> contactList = userContactService.findListByParam(query);
        //TODO 向对方发送申请对方没同意过就被被拉黑的，则对方不显示在自己的联系人列表中
        return getSuccessResponseVo(contactList);
    }

    /**
     * 加载联系人详情-1.将鼠标悬停在联系人头像上显示的详情2.在群组里查看群成员信息的接口
     *
     * @param request
     * @param contactId
     * @return
     */
    @RequestMapping("/getContactInfo")
    @GlobalInterceptor
    public ResponseVo getContactInfo(HttpServletRequest request, @NotNull String contactId) {
        TokenUserInfoDTO tokenUserInfoDTO = getTokenUserInfo(request);
        UserInfo userInfo = userInfoService.getUserInfoByUserId(contactId);
        UserInfoVo userInfoVo = CopyUtils.copy(userInfo, UserInfoVo.class);
        userInfoVo.setContactStatus(UserContactStatusEnum.NOT_FRIEND.getStatus());
        UserContact userContact = userContactService.getUserContactByUserIdAndContactId(tokenUserInfoDTO.getUserId(), contactId);
        if (null != userContact) {
            userInfoVo.setContactStatus(UserContactStatusEnum.FRIEND.getStatus());
        }
        return getSuccessResponseVo(userInfoVo);
    }

    /**
     * 加载联系人详情-在联系人列表点击联系人显示的详情
     *
     * @param request
     * @param contactId
     * @return
     */
    @RequestMapping("/getContactUserInfo")
    @GlobalInterceptor
    public ResponseVo getContactUserInfo(HttpServletRequest request, @NotNull String contactId) throws BusinessException {
        TokenUserInfoDTO tokenUserInfoDTO = getTokenUserInfo(request);
        UserInfo userInfo = userInfoService.getUserInfoByUserId(contactId);
        UserInfoVo userInfoVo = CopyUtils.copy(userInfo, UserInfoVo.class);
        UserContact userContact = userContactService.getUserContactByUserIdAndContactId(tokenUserInfoDTO.getUserId(), contactId);
        if (null == userContact || !ArrayUtils.contains(new Integer[]{UserContactStatusEnum.FRIEND.getStatus(),
                                                                        UserContactStatusEnum.BLACKLIST_BE.getStatus(),
                                                                        UserContactStatusEnum.DEL_BE.getStatus()}, userContact.getStatus())) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        return getSuccessResponseVo(userInfoVo);
    }

    /**
     * 删除联系人
     *
     * @param request
     * @param contactId
     * @return
     */
    @RequestMapping("/delContact")
    @GlobalInterceptor
    public ResponseVo delContact(HttpServletRequest request, @NotNull String contactId) {
        TokenUserInfoDTO tokenUserInfoDTO = getTokenUserInfo(request);
        userContactService.removeUserContact(tokenUserInfoDTO.getUserId(), contactId, UserContactStatusEnum.DEL);
        return getSuccessResponseVo(null);
    }

    /**
     * 拉黑联系人
     *
     * @param request
     * @param contactId
     * @return
     */
    @RequestMapping("/addContact2BlackList")
    @GlobalInterceptor
    public ResponseVo addContact2BlackList(HttpServletRequest request, @NotNull String contactId) {
        TokenUserInfoDTO tokenUserInfoDTO = getTokenUserInfo(request);
        userContactService.removeUserContact(tokenUserInfoDTO.getUserId(), contactId, UserContactStatusEnum.BLACKLIST);
        return getSuccessResponseVo(null);
    }


}
