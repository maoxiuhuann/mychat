package com.ezchat.enums;

public enum ResponseCodeEnum {
    CODE_200(200,"请求成功"),
    CODE_404(404,"请求资源不存在"),
    CODE_600(600,"请求参数错误"),
    CODE_601(601,"信息已经存在"),
    CODE_602(602,"文件不存在"),
    CODE_500(500,"服务器内部错误,请联系管理员"),
    CODE_901(901,"登录超时"),
    CODE_902(902,"您不是对方好友"),
    CODE_903(903,"您已被移出群聊或被禁言");


    private Integer code;

    private String msg;

    ResponseCodeEnum(Integer code, String msg){
        this.code = code;
        this.msg = msg;
    }


    public Integer getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }
}

