package com.bilibili.cluster.scheduler.dao.datasource;

public class DynamicDataSourceHolder {

    /**
     * 注意：数据源标识保存在线程变量中，避免多线程操作数据源时互相干扰
     */
    private static final ThreadLocal<DataSourceKey> THREAD_DATA_SOURCE = new ThreadLocal<DataSourceKey>();

    public static DataSourceKey getDataSource() {
        return THREAD_DATA_SOURCE.get();
    }

    public static void setDataSource(DataSourceKey dataSource) {

        THREAD_DATA_SOURCE.set(dataSource);
    }

    public static void clearDataSource() {

        THREAD_DATA_SOURCE.remove();
    }
}

