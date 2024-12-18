package com.ezchat.enums;

public enum GroupStatusEnum {

    NORMAL(1, "正常"),

    DISSOLUTION(0, "已解散");

    public Integer status;
    private String desc;

    GroupStatusEnum(Integer status, String desc) {
        this.status = status;
        this.desc = desc;
    }

    public static GroupStatusEnum getBuStatus(Integer status) {
        for (GroupStatusEnum item : GroupStatusEnum.values()) {
            if (item.status.equals(status)) {
                return item;
            }
        }
        return null;
    }

    public Integer getStatus() {
        return status;
    }

    public String getDesc() {
        return desc;
    }
}
