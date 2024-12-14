package com.ezchat.enums;

public enum VipAccountStatusEnum {
        NO_USE(0, "未使用"),
        USED(1, "已使用");

        private Integer status;
        private String desc;

    VipAccountStatusEnum(Integer status, String desc) {
            this.status = status;
            this.desc = desc;
        }

        public static VipAccountStatusEnum getByStatus(Integer status) {
            for (VipAccountStatusEnum item : VipAccountStatusEnum.values()) {
                if (item.getStatus().equals(status)) {
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

        public void setDesc(String desc){
            this.desc = desc;
        }
}
