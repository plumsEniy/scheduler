package com.bilibili.cluster.scheduler.common.dto.spark.client;

import cn.hutool.core.annotation.Alias;
import com.bilibili.cluster.scheduler.common.Constants;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

@Data
public class SparkClientPackInfo implements Comparable<SparkClientPackInfo> {

    @Alias(value = Constants.SPARK_CLIENT_PACK_DOWNLOAD_URL_KEY)
    private String downloadUrl;

    @Alias(value = Constants.SPARK_CLIENT_PACK_MD5_KEY)
    private String packMd5;

    @Alias(value = Constants.SPARK_CLIENT_PACK_NAME_KEY)
    private String packName;

    @Alias(value = Constants.SPARK_CLIENT_PACK_TYPE_KEY)
    private String clientType;

    @Override
    public int compareTo(@NotNull SparkClientPackInfo o) {
        int compare = o.clientType.compareTo(this.clientType);
        if (compare == 0) {
            compare = o.packName.compareTo(this.packName);
        }
        return compare;
    }

}
