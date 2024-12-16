package com.ezchat.controller;

import com.ezchat.annotation.GlobalInterceptor;
import com.ezchat.constans.Constans;
import com.ezchat.entity.vo.ResponseVo;

import com.ezchat.entity.vo.UserInfoVo;
import com.ezchat.exception.BusinessException;
import com.ezchat.redis.RedisComponent;
import com.ezchat.redis.RedisUtils;
import com.ezchat.service.UserInfoService;
import com.wf.captcha.ArithmeticCaptcha;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


@RestController("accountController")
@RequestMapping("/account")
@Validated
public class AccountController extends ABaseController {

    private static final Logger logger = LoggerFactory.getLogger(AccountController.class);

    @Autowired
    private RedisUtils redisUtils;

    @Autowired
    private UserInfoService userInfoService;

    @Autowired
    private RedisComponent redisComponent;

    /**
     * 登录注册-验证码逻辑
     *
     * @return
     */
    @RequestMapping("/checkCode")
    public ResponseVo checkCode() {
        //生成验证码图片
        ArithmeticCaptcha captcha = new ArithmeticCaptcha(100, 42);
        //生成验证码结果
        String code = captcha.text();
        //给每次验证码code设置唯一key，存入redis并设置过期时间
        String checkCodeKey = UUID.randomUUID().toString();
        redisUtils.setex(Constans.REDIS_KEY_CHECK_CODE + checkCodeKey, code, Constans.REDIS_TIME_1MIN * 10);
        //图片转base64，方便传给前端
        String checkCodeBase64 = captcha.toBase64();
        //规范做法是定义一个vo接收
        Map<String, String> result = new HashMap<>();
        result.put("checkCodeBase64", checkCodeBase64);
        result.put("checkCodeKey", checkCodeKey);
        return getSuccessResponseVo(checkCodeBase64);
    }

    /**
     * 登录注册-注册逻辑
     *
     * @return
     */
    @RequestMapping("/register")
    public ResponseVo register(@NotEmpty String checkCodeKey,
                               @NotEmpty @Email String email,
                               @NotEmpty String password,
                               @NotEmpty String nickName,
                               @NotEmpty String checkCode) throws BusinessException {
        try {
            if (!checkCode.equalsIgnoreCase((String) redisUtils.get(Constans.REDIS_KEY_CHECK_CODE + checkCodeKey))) {
                throw new BusinessException("图片验证码错误");
            }
            userInfoService.register(email, nickName, password);
            return getSuccessResponseVo(null);
        } finally {
            redisUtils.delete(Constans.REDIS_KEY_CHECK_CODE + checkCodeKey);
        }
    }

    /**
     * 登录注册-登录逻辑
     *
     * @return
     */
    @RequestMapping("/login")
    public ResponseVo login(@NotEmpty String checkCodeKey,
                                @NotEmpty @Email String email,
                                @NotEmpty String password,
                                @NotEmpty String checkCode) throws BusinessException {
        try {
            if (!checkCode.equalsIgnoreCase((String) redisUtils.get(Constans.REDIS_KEY_CHECK_CODE + checkCodeKey))) {
                throw new BusinessException("图片验证码错误");
            }
            UserInfoVo userInfoVo = userInfoService.login(email, password);
            return getSuccessResponseVo(null);
        } finally {
            redisUtils.delete(Constans.REDIS_KEY_CHECK_CODE + checkCodeKey);
        }
    }

    /**
     * 获取系统设置
     *
     * @return
     * @throws BusinessException
     */
    @GlobalInterceptor
    @RequestMapping("/getSysSetting")
    public ResponseVo getSysSettings() throws BusinessException {
        return getSuccessResponseVo(redisComponent.getSysSetting());
    }
}
