import cn.hutool.json.JSONUtil;
import com.bilibili.cluster.scheduler.api.ApiApplicationServer;
import com.bilibili.cluster.scheduler.api.service.oa.OAService;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.oa.OAForm;
import com.bilibili.cluster.scheduler.common.dto.oa.manager.OaChangeInfo;
import com.bilibili.cluster.scheduler.common.dto.oa.manager.ReplaceRoleModel;
import com.bilibili.cluster.scheduler.common.dto.spark.params.SparkDeployFlowExtParams;
import com.bilibili.cluster.scheduler.common.dto.spark.params.SparkDeployType;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowDeployType;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;

/**
 * @description:审批单的测试
 * @Date: 2024/3/6 16:01
 * @Author: nizhiqiang
 */

@RunWith(SpringRunner.class)
@SpringBootTest(classes = ApiApplicationServer.class)
@Slf4j
public class OATest {

    @Resource
    OAService oaService;

    @Test
    public void testSubmitAndQuery() {
        // OAForm oaForm = oaService.submitFlinkManagerForm("xuchen", "OA流程测试");
        final OaChangeInfo changeInfo = new OaChangeInfo();
        changeInfo.setEnv("pre");
        changeInfo.setChangeType(FlowDeployType.SPARK_DEPLOY.getDesc());
        changeInfo.setChangeComponent("Spark-Manager(测试工单1)");

        final StringBuilder detailBuilder = new StringBuilder();

        SparkDeployFlowExtParams sparkDeployFlowExtParams = new SparkDeployFlowExtParams();
        sparkDeployFlowExtParams.setSparkDeployType(SparkDeployType.EMERGENCY);
        sparkDeployFlowExtParams.setTargetSparkVersion("spark-3.3.1.5");

        detailBuilder.append("发布场景:").append(sparkDeployFlowExtParams.getSparkDeployType().getDesc()).append(Constants.NEW_LINE);
        detailBuilder.append("发布范围:").append("全量发布").append(Constants.NEW_LINE);
        detailBuilder.append("应用版本:").append(sparkDeployFlowExtParams.getTargetSparkVersion()).append(Constants.NEW_LINE);
        changeInfo.setRemark(detailBuilder.toString());

        OAForm oaForm = oaService.submitUnifiedForm("xuchen", Arrays.asList("nizhiqiang", "liuguohui"), Arrays.asList("wangchao12"), null,
                Constants.BMR_UNIFIED_OA_PROCESS_NAME, changeInfo, LocalDateTime.now().minusDays(1).toEpochSecond(ZoneOffset.of("+08")),
                "infra.alter.spark-manager", () -> {
                    final ReplaceRoleModel replaceRoleModel = new ReplaceRoleModel();
                    replaceRoleModel.setDevLeader("zhangyang01,zhangwei24,nizhiqiang".split(","));
                    replaceRoleModel.setSreLeader("hukai,liuminggang,liuguohui,nizhiqiang".split(","));
                    return replaceRoleModel;
                });

//        OAForm oaForm = oaService.submitUnifiedForm("xuchen", Arrays.asList("nizhiqiang", "liuguohui", "xuchen"), Arrays.asList("wangchao12"), null,
//                Constants.BMR_UNIFIED_OA_PROCESS_NAME, changeInfo, LocalDateTime.now().minusDays(1).toEpochSecond(ZoneOffset.of("+08")),
//                "infra.alter.spark-manager");


        System.out.println("submit oaForm = " + JSONUtil.toJsonStr(oaForm));

        String orderId = oaForm.getOrderId();
        oaForm = oaService.queryForm("xuchen", orderId);
        System.out.println("query oaForm = " + oaForm);
    }
}
