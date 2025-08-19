package com.bilibili.cluster.scheduler.common.dto.yarn.req;

import com.bilibili.cluster.scheduler.common.enums.bmr.config.FileOperateType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateFileIpListReq {

    private long componentId;
    private String fileName;
    private FileOperateType operateType;
    private List<String> ipList;

}
