package com.bilibili.cluster.scheduler.common.enums;



public enum BizTypeEnums {

    ARCHER(0, "数据开发-Archer"),
    RIDER(1, "数据集成批同步-Rider"),
    TAG(4, "标签服务-Titan"),
    ICEBERG(5, "数据湖-Iceberg"),
    SABER(6, "流计算-Saber"),
    AIRFLOW(7, "AI部门AirFlow"),
    LANCER_DUMMY(8, "数据集成实时流-Lancer"),
    AVENGER(9, "数据盘-Avenger"),
    ONE_SERVICE(10, "one service"),
    DW_HISTORY(11, "dw one history"),
    VIEW_DUMMY_JOB(12, "元数据视图虚任务"),
    POLARIS(13, "北极星后台应用"),
    GOVERNA(14, "数据治理应用"),
    FLINK_CDC(15, "flink-cdc业务"),
    HUDI_MANAGER(16, "hudi manager");

    private String msg;
    private Integer code;

    BizTypeEnums(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public static BizTypeEnums getByCode(Integer code) {
        for (BizTypeEnums bizType : BizTypeEnums.values()) {
            if (bizType.code.equals(code)) {
                return bizType;
            }
        }
        throw new IllegalArgumentException("该code没有对应的biztype,code为:" + code);
    }

    public String getMsg() {
        return msg;
    }

    public int getCode() {
        return code;
    }
}
