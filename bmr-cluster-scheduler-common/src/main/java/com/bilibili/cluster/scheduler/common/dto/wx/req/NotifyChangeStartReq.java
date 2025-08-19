package com.bilibili.cluster.scheduler.common.dto.wx.req;

import cn.hutool.core.date.DateUtil;
import com.bilibili.cluster.scheduler.common.Constants;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * @description: 通知变更开始
 * @Date: 2024/3/18 18:15
 * @Author: nizhiqiang
 */
@Data
public class NotifyChangeStartReq {

    // 1:运维操作；2：产品迭代
    private int ops;
    //发起人
    private String sponsor;
    //审批人
    private String dealer;
    //操作人
    private String operator;
    //团队id
    private int team;
    //组件名称
    private String component;
    // 预计开始时间
    private long expect_start_at;
    //预计结束时间
    private long expect_end_at;
    //变更内容
    private String changelog;
    //组件id
    private int component_id;
    //是否使用默认机器人
    private boolean use_default_robot;
    //选中机器人的id组
    private List<Integer> selected_robots;

    private NotifyChangeStartReq(int ops, String sponsor, String dealer, String operator, int team, String component, int componentId, boolean useDefaultRobot, List<Integer> selectedRobots) {
        this.ops = ops;
        this.sponsor = sponsor;
        this.dealer = dealer;
        this.operator = operator;
        this.team = team;
        this.component = component;
        this.component_id = componentId;
        this.use_default_robot = useDefaultRobot;
        this.selected_robots = selectedRobots;
        this.expect_start_at = new Date().getTime() / 1000;
        this.expect_end_at = DateUtil.offsetMinute(new Date(), 60).getTime() / 1000;
    }

    public static NotifyChangeStartReq getModel(String username, List<Integer> selectedRobotList) {
        NotifyChangeStartReq model = new NotifyChangeStartReq(1, username, username, username,
                Constants.NOTIFY_TEAM, Constants.NOTIFY_COMPOMENT, Constants.NOTIFY_COMPOMENT_ID, false, selectedRobotList);
        return model;
    }

}
