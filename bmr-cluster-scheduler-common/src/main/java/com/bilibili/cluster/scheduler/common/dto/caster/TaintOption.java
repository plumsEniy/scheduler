package com.bilibili.cluster.scheduler.common.dto.caster;

import lombok.Data;

@Data
public class TaintOption {

    private TaintOperation operation;

    private TaintConf taint;

}
