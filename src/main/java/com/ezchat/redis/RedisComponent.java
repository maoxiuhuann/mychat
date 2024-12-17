package com.ezchat.redis;

import com.ezchat.constans.Constans;
import com.ezchat.entity.dto.SysSettingDTO;
import com.ezchat.entity.dto.TokenUserInfoDTO;
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
        redisUtils.setex(Constans.REDIS_KEY_WS_TOKEN + tokenUserInfoDTO.getToken(), tokenUserInfoDTO, Constans.REDIS_TIME_1DAY / 2);
        //可以通过userid获取token
        redisUtils.setex(Constans.REDIS_KEY_WS_TOKEN_USERID + tokenUserInfoDTO.getToken(), tokenUserInfoDTO.getToken(), Constans.REDIS_TIME_1DAY / 2);
    }

    public SysSettingDTO getSysSetting() {
        SysSettingDTO sysSettingDTO = (SysSettingDTO) redisUtils.get(Constans.REDIS_KEY_SYS_SETTING);
        sysSettingDTO = sysSettingDTO == null ? new SysSettingDTO() : sysSettingDTO;
        return sysSettingDTO;
    }
}
