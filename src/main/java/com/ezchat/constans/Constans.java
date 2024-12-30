package com.ezchat.constans;

import com.ezchat.enums.UserContactTypeEnum;

public class Constans {
    //redis过期时间常量
    public static final Integer REDIS_TIME_1MIN = 60;

    public static final Integer REDIS_TIME_1DAY = 60 * 60 * 24;

    public static final Integer LENGTH_11 = 11;

    public static final Integer LENGTH_20 = 20;

    //存验证码结果的key
    public static final String REDIS_KEY_CHECK_CODE = "ezchat:checkcode";

    //存用户心跳的key
    public static final String REDIS_KEY_WS_USER_HEART_BEAT = "ezchat:ws:user:heartbeat";

    //存用户token的key
    public static final String REDIS_KEY_WS_TOKEN = "ezchat:ws:token";

    public static final String REDIS_KEY_WS_TOKEN_USERID = "ezchat:ws:token:userid";

    //存系统设置的key
    public static final String REDIS_KEY_SYS_SETTING = "ezchat:syssetting";

    public static final String ROBOT_UID = UserContactTypeEnum.USER.getPrefix() + "ROBOT";

    //文件上传的存放位置常亮
    public static final String FILE_FOLDER_FILE = "file/";

    public static final String FILE_FOLDER_AVATAR_NAME = "avatar/";

    public static final String IMAGE_SUFFIX = ".png";

    public static final String COVER_IMAGE_SUFFIX = "_cover.png";

    //APP文件目录
    public static final String APP_UPDATE_FOLDER = "app/";

    public static final String APP_EXE_SUFFIX = ".exe";

    //默认申请理由
    public static final String DEFAULT_APPLY_REASON = "我是%s";

    //密码正则表达式-由字母、数字、特殊字符，任意2种组成，1-9位
    public static final String REGEX_PASSWORD = "^(?![a-zA-Z]+$)(?!\\d+$)(?![^\\da-zA-Z\\s]+$).{1,9}$";
}
