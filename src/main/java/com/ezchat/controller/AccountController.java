package com.ezchat.controller;

import com.ezchat.constans.Constans;
import com.ezchat.entity.vo.ResponseVo;

import com.ezchat.redis.RedisUtils;
import com.wf.captcha.ArithmeticCaptcha;


import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

@RestController("accountController")
@RequestMapping("/account")
@Validated
public class AccountController extends ABaseController {

    private static final Logger logger = (Logger) LoggerFactory.getLogger(AccountController.class);

    @Autowired
    private RedisUtils redisUtils;

    @RequestMapping("/checkCode")
    public ResponseVo checkCode() {
        ArithmeticCaptcha captcha = new ArithmeticCaptcha(100, 43);
        String code = captcha.text();
        String checkCodeKey = UUID.randomUUID().toString();
        redisUtils.setex(Constans.REDIS_KEY_CHECK_CODE,code,Constans.REDIS_TIME_1MIN * 10);
        String checkCodeBase64 = captcha.toBase64();
        Map<String, String> result = new HashMap<>();
        result.put("checkCodeBase64", checkCodeBase64);
        result.put("checkCodeKey", checkCodeKey);
        return getSuccessResponseVo(checkCodeBase64);
    }
}
