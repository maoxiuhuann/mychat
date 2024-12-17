package com.ezchat.aspect;


import com.ezchat.annotation.GlobalInterceptor;
import com.ezchat.constans.Constans;
import com.ezchat.entity.dto.TokenUserInfoDTO;
import com.ezchat.enums.ResponseCodeEnum;
import com.ezchat.exception.BusinessException;
import com.ezchat.redis.RedisUtils;
import com.ezchat.utils.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;

@Aspect
@Component("globalOperationAspect")
public class GlobalOperationAspect {

    @Autowired
    private RedisUtils redisUtils;

    private static final Logger logger = LoggerFactory.getLogger(GlobalOperationAspect.class);


    /**
     * AOP 切面，拦截所有带有 @GlobalInterceptor 注解的方法，并进行登录校验，判断是否登录以及是否有管理员权限。
     * 使用 @annotation(com.ezchat.annotation.GlobalInterceptor) 作为切入点，表示切面将拦截所有带有 @GlobalInterceptor 注解的方法。使用 @Before 通知，表示拦截器会在目标方法执行之前运行。
     * @param point
     * @throws BusinessException
     */
    @Before("@annotation(com.ezchat.annotation.GlobalInterceptor)")
    public void interceptDo(JoinPoint point) throws BusinessException {
        try {
            //使用 AOP 提供的 MethodSignature 获取被拦截方法上的注解 GlobalInterceptor，以便读取其配置参数。
            Method method = ((MethodSignature) point.getSignature()).getMethod();
            GlobalInterceptor interceptor = method.getAnnotation(GlobalInterceptor.class);
            //如果被拦截方法上没有配置 GlobalInterceptor 注解，则直接返回不做任何校验操作。
            if(interceptor == null){
                return;
            }
            //如果注解配置了 checkLogin 或 checkAdmin 为 true，则调用 checkLogin() 方法进行校验。
            if (interceptor.checkLogin() || interceptor.checkAdmin()){
                checkLoginAndAdmin(interceptor.checkAdmin());
            }
        }catch (BusinessException e){
            logger.error("全局拦截异常",e);
        }catch (Exception e){
            logger.error("全局拦截异常",e);
            throw new BusinessException(ResponseCodeEnum.CODE_500);
        }catch (Throwable e){
            logger.error("全局拦截异常",e);
            throw new BusinessException(ResponseCodeEnum.CODE_500);
        }

    }

    private void checkLoginAndAdmin(Boolean checkAdmin) throws BusinessException {
        //使用 RequestContextHolder 获取当前 HTTP 请求的上下文，通过 request.getHeader("token") 获取前端传递的 token。
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String token = request.getHeader("token");
        //如果 token 为空，抛出业务异常 CODE_901，表示用户未登录。在redis之前操作，减少一次redis查询
        if(StringUtils.isEmpty(token)){
            throw new BusinessException(ResponseCodeEnum.CODE_901);
        }
        TokenUserInfoDTO tokenUserInfoDTO = (TokenUserInfoDTO) redisUtils.get(Constans.REDIS_KEY_WS_TOKEN + token);
        //如果 Redis 中未找到对应的 tokenUserInfoDTO，说明登录已过期。
        if(tokenUserInfoDTO == null){
            throw new BusinessException(ResponseCodeEnum.CODE_901);
        }
        //如果注解配置了 checkAdmin = true，且当前用户信息的 getAdmin() 返回值为 false，抛出业务异常 CODE_404，表示无管理员权限。
        if (checkAdmin && !tokenUserInfoDTO.getAdmin()){
            throw new BusinessException(ResponseCodeEnum.CODE_404);
        }
    }
}
