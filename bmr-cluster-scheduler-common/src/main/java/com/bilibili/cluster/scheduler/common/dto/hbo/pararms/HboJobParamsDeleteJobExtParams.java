package com.bilibili.cluster.scheduler.common.dto.hbo.pararms;

import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.bilibili.cluster.scheduler.common.dto.params.BaseNodeParams;
import lombok.Data;

/**
 * @description:
 * @Date: 2024/12/30 11:27
 * @Author: nizhiqiang
 */

@Data
public class HboJobParamsDeleteJobExtParams extends BaseNodeParams {

    String beforeParams = Constants.EMPTY;

}
