import com.bilibili.cluster.scheduler.api.ApiApplicationServer;
import com.bilibili.cluster.scheduler.api.service.bmr.flow.BmrFlowService;
import com.bilibili.cluster.scheduler.api.service.bmr.metadata.BmrMetadataService;
import com.bilibili.cluster.scheduler.common.dto.bmr.metadata.MetadataComponentData;
import com.bilibili.cluster.scheduler.common.entity.BmrFlowEntity;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.List;

/**
 * @description: bmr测试
 * @Date: 2024/5/23 11:23
 * @Author: nizhiqiang
 */

@RunWith(SpringRunner.class)
@SpringBootTest(classes = ApiApplicationServer.class)
@Slf4j
public class BmrTest {

    @Resource
    BmrFlowService bmrFlowService;

    @Resource
    BmrMetadataService bmrMetadataService;


    @Test
    public void testBmrFlow(){
        BmrFlowEntity flow = bmrFlowService.getById(1);
        System.out.println("flow = " + flow);
    }

    @Test
    public void testBmrMetadata(){
        System.out.println("bmrMetadataService.queryComponentListByClusterId(1) = " + bmrMetadataService.queryComponentListByClusterId(1));
        System.out.println("bmrMetadataService.queryComponentByComponentId(1) = " + bmrMetadataService.queryComponentByComponentId(1));
        System.out.println("bmrMetadataService.queryPackageDetailById(1) = " + bmrMetadataService.queryPackageDetailById(1));
        System.out.println("bmrMetadataService.queryPackageDownloadInfo(1) = " + bmrMetadataService.queryPackageDownloadInfo(1));
        System.out.println("bmrMetadataService.queryVariableByComponentId(1) = " + bmrMetadataService.queryVariableByComponentId(1));
        System.out.println("bmrMetadataService.queryClusterDetail(1) = " + bmrMetadataService.queryClusterDetail(1));
    }


}
