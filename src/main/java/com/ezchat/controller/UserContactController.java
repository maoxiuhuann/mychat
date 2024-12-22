package com.ezchat.controller;


import com.ezchat.annotation.GlobalInterceptor;
import com.ezchat.entity.dto.TokenUserInfoDTO;
import com.ezchat.entity.dto.UserContactSearchResultDTO;
import com.ezchat.entity.query.UserContactApplyQuery;
import com.ezchat.entity.vo.PaginationResultVO;
import com.ezchat.entity.vo.ResponseVo;
import com.ezchat.enums.PageSize;
import com.ezchat.exception.BusinessException;
import com.ezchat.service.UserContactApplyService;
import com.ezchat.service.UserContactService;
import com.ezchat.service.UserInfoService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

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
}
