package com.ezchat.enums;

public enum GroupControlOpTypeEnum {
    REMOVE_USER(0),
    ADD_USER(1);

    private Integer type;

    GroupControlOpTypeEnum(int type) {
        this.type = type;
    }

    public Integer getType() {
        return type;
    }

    public static GroupControlOpTypeEnum getType(int type) {
        for (GroupControlOpTypeEnum opTypeEnum : GroupControlOpTypeEnum.values()){
            if (opTypeEnum.type.equals(type)){
                return  opTypeEnum;
            }
        }
        return null;
    }

}
