package com.ezchat.redis;

import com.ezchat.constans.Constans;
import com.ezchat.entity.dto.SysSettingDTO;
import com.ezchat.entity.dto.TokenUserInfoDTO;
import jdk.nashorn.internal.parser.Token;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component("redisComponent")
public class RedisComponent {

    @Resource
    private RedisUtils redisUtils;

    /**
     * 获取用户心跳
     *
     * @param userId
     * @return
     */
    public Long getUserHeartBeat(String userId) {
        return (Long) redisUtils.get(Constans.REDIS_KEY_WS_USER_HEART_BEAT + userId);
    }

    /**
     * 保存用户token
     *
     * @param tokenUserInfoDTO
     */
    public void saveTokenUserInfoDTO(TokenUserInfoDTO tokenUserInfoDTO) {
        //可以通过token获取tokenUserInfoDTO
        redisUtils.setex(Constans.REDIS_KEY_WS_TOKEN + tokenUserInfoDTO.getToken(), tokenUserInfoDTO, Constans.REDIS_TIME_1DAY);
        //可以通过userid获取token
        redisUtils.setex(Constans.REDIS_KEY_WS_TOKEN_USERID + tokenUserInfoDTO.getUserId(), tokenUserInfoDTO.getToken(), Constans.REDIS_TIME_1DAY);
    }

    /**
     * 获取系统设置信息
     * @return
     */
    public SysSettingDTO getSysSetting() {
        SysSettingDTO sysSettingDTO = (SysSettingDTO) redisUtils.get(Constans.REDIS_KEY_SYS_SETTING);
        sysSettingDTO = sysSettingDTO == null ? new SysSettingDTO() : sysSettingDTO;
        return sysSettingDTO;
    }

    /**
     * 保存系统设置信息
     * @param sysSettingDTO
     */
    public void saveSysSetting(SysSettingDTO sysSettingDTO) {
        redisUtils.set(Constans.REDIS_KEY_SYS_SETTING, sysSettingDTO);
    }

    /**
     * 获取token对应的用户信息
     * @param token
     * @return
     */
    public TokenUserInfoDTO getTokenUserInfoDTO(String token) {
        TokenUserInfoDTO tokenUserInfoDTO = (TokenUserInfoDTO) redisUtils.get(Constans.REDIS_KEY_WS_TOKEN + token);
        return tokenUserInfoDTO;
    }
}
