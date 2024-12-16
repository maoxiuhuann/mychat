package com.ezchat.controller;

import com.ezchat.annotation.GlobalInterceptor;
import com.ezchat.entity.dto.TokenUserInfoDTO;
import com.ezchat.entity.po.GroupInfo;
import com.ezchat.entity.query.GroupInfoQuery;
import com.ezchat.entity.vo.ResponseVo;
import com.ezchat.service.GroupInfoService;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @Description:Controller
 * @author:xiuyuan
 * @date:2024/12/16
 */
@RestController
@RequestMapping("/group")
public class GroupInfoController extends ABaseController {

	@Resource
	private GroupInfoService groupInfoService;

	@RequestMapping("/saveGroup")
	@GlobalInterceptor
	public ResponseVo saveGroup(HttpServletRequest request,
								String groupId,
								@NotEmpty String groupName,
								String groupNotice,
								@NotNull String joinType,
								MultipartFile avatarFile,//头像文件-原图和缩略图
								MultipartFile avatarCover) {

		//从header中获取token、使用AOP保证token不为空即用户已经登陆
		TokenUserInfoDTO tokenUserInfoDTO = getTokenUserInfo(request);
		return getSuccessResponseVo(null);
	}

}
