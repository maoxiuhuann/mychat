package com.ezchat.controller;


import com.ezchat.annotation.GlobalInterceptor;
import com.ezchat.entity.dto.TokenUserInfoDTO;
import com.ezchat.entity.dto.UserContactSearchResultDTO;
import com.ezchat.entity.vo.ResponseVo;
import com.ezchat.exception.BusinessException;
import com.ezchat.service.UserContactApplyService;
import com.ezchat.service.UserContactService;
import com.ezchat.service.UserInfoService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotEmpty;

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

}
