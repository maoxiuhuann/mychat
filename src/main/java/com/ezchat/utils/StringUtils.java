package com.ezchat.utils;

import com.ezchat.constans.Constans;
import com.ezchat.enums.UserContactTypeEnum;
import com.ezchat.exception.BusinessException;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.RandomStringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class StringUtils {
    public static void checkParam(Object param) throws BusinessException {
        try {
            Field[] fields = param.getClass().getDeclaredFields();
            boolean notEmpty = false;
            for (Field field : fields) {
                String methodName = "get" + StringUtils.upperCaseFirstLetter(field.getName());
                Method method = param.getClass().getMethod(methodName);
                Object object = method.invoke(param);
                if (object != null && object instanceof java.lang.String && !StringUtils.isEmpty(object.toString())
                        || object != null && !(object instanceof java.lang.String)) {
                    notEmpty = true;
                    break;
                }
            }
            if (!notEmpty) {
                throw new BusinessException("参数为空");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new BusinessException("校验参数是否为空失败");
        }
    }

    public static String upperCaseFirstLetter(String field) {
        if (isEmpty(field)) {
            return field;
        }
        // 如果第二个字母是大写，第一个字母不大写
        if (field.length() > 1 && Character.isUpperCase(field.charAt(1))) {
            return field;
        }
        return field.substring(0, 1).toUpperCase() + field.substring(1);
    }

    public static boolean isEmpty(String str) {
        if (null == str || "".equals(str) || "null".equals(str) || "\u0000".equals(str)) {
            return true;
        } else if (" ".equals(str.trim())) {
            return true;
        }
        return false;
    }

    /**
     * 生成12位用户ID
     *
     * @return
     */
    public static String getUserId() {
        //添加前缀区别用户账号和群账号
        return UserContactTypeEnum.USER.getPrefix() + getRandomNumber(Constans.LENGTH_11);
    }

    /**
     * 生成12位群ID
     *
     * @return
     */
    public static String getGroupId() {
        //添加前缀区别用户账号和群账号
        return UserContactTypeEnum.GROUP.getPrefix() + getRandomNumber(Constans.LENGTH_11);
    }

    //生成11位随机数，作为用户账号
    public static String getRandomNumber(Integer count) {
        return RandomStringUtils.random(count, false, true);
    }

    public static String getRandomString(Integer count) {
        return RandomStringUtils.random(count, true, true);
    }

    //md5加密
    public static String encodeMd5(String str) {
        return StringUtils.isEmpty(str) ? null : DigestUtils.md5Hex(str);
    }
}
