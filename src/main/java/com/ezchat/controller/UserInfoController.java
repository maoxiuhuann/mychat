package com.ezchat.controller;

import com.ezchat.annotation.GlobalInterceptor;
import com.ezchat.constans.Constans;
import com.ezchat.entity.dto.TokenUserInfoDTO;
import com.ezchat.entity.po.UserInfo;
import com.ezchat.entity.vo.ResponseVo;
import com.ezchat.entity.vo.UserInfoVo;
import com.ezchat.enums.UserContactStatusEnum;
import com.ezchat.service.UserInfoService;
import com.ezchat.utils.CopyUtils;
import com.ezchat.utils.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.io.IOException;

@RequestMapping("/userInfo")
@RestController
public class UserInfoController extends ABaseController{

    @Resource
    private UserInfoService userInfoService;

    /**
     * 获取用户自己账号信息 -po数据库、vo返回给前端视图、dto不同类之间的数据传输对象
     * @param request
     * @return
     */
    @RequestMapping("/getUserInfo")
    @GlobalInterceptor
    public ResponseVo getUserInfo(HttpServletRequest request) {
        TokenUserInfoDTO tokenUserInfoDTO = getTokenUserInfo(request);
        UserInfo userInfo = userInfoService.getUserInfoByUserId(tokenUserInfoDTO.getUserId());
        UserInfoVo userInfoVo = CopyUtils.copy(userInfo, UserInfoVo.class);
        userInfoVo.setAdmin(tokenUserInfoDTO.getAdmin());
        return getSuccessResponseVo(userInfoVo);
    }


    /**
     * 更新用户信息
     * @param request
     * @param userInfo
     * @param avatarFile
     * @param avatarCover
     * @return
     */
    @RequestMapping("/saveUserInfo")
    @GlobalInterceptor
    public ResponseVo saveUserInfo(HttpServletRequest request, UserInfo userInfo,
                                   MultipartFile avatarFile,
                                   MultipartFile avatarCover) throws IOException {
        TokenUserInfoDTO tokenUserInfoDTO = getTokenUserInfo(request);
        userInfo.setUserId(tokenUserInfoDTO.getUserId());
        //这些信息不应该被修改-UserInfoMapper中设置了为空时不修改
        userInfo.setPassword(null);
        userInfo.setStatus(null);
        userInfo.setCreateTime(null);
        userInfo.setLastLoginTime(null);

        this.userInfoService.updateUserInfo(userInfo, avatarFile, avatarCover);

        return getUserInfo(request);
    }

    /**
     * 更新用户密码
     * @param request
     * @param password
     * @return
     */
    @RequestMapping("/updatePassword")
    @GlobalInterceptor
    public ResponseVo updatePassword(HttpServletRequest request, @NotEmpty @Pattern(regexp = Constans.REGEX_PASSWORD) String password) {
        TokenUserInfoDTO tokenUserInfoDTO = getTokenUserInfo(request);
        UserInfo userInfo = new UserInfo();
        userInfo.setPassword(StringUtils.encodeMd5(password));
        userInfoService.updateUserInfoByUserId(userInfo, tokenUserInfoDTO.getUserId());
        //todo 修改密码之后需要强制退出重新登录
        return getUserInfo(null);
    }

    /**
     * 退出登录
     * @param request
     * @param password
     * @return
     */
    @RequestMapping("/logout")
    @GlobalInterceptor
    public ResponseVo logout(HttpServletRequest request, @NotEmpty @Pattern(regexp = Constans.REGEX_PASSWORD) String password) {
        TokenUserInfoDTO tokenUserInfoDTO = getTokenUserInfo(request);
        //todo 退出登录，关闭ws连接
        return getUserInfo(null);
    }
}
