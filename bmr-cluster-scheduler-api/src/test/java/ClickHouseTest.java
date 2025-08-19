import cn.hutool.json.JSONUtil;
import com.bilibili.cluster.scheduler.api.ApiApplicationServer;
import com.bilibili.cluster.scheduler.api.service.clickhouse.clickhouse.ClickhouseService;
import com.bilibili.cluster.scheduler.common.dto.clickhouse.clickhouse.ClickhouseDeployDTO;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.Arrays;

/**
 * @description:
 * @Date: 2025/2/8 15:13
 * @Author: nizhiqiang
 */

@RunWith(SpringRunner.class)
@SpringBootTest(classes = ApiApplicationServer.class)
@Slf4j
public class ClickHouseTest {

    @Resource
    ClickhouseService clickhouseService;

    @Test
    public void testBuildDeployDTO() {
        ClickhouseDeployDTO clickhouseDeployDTO = clickhouseService.buildClickhouseDeployDTO(2515L);
        System.out.println("clickhouse1 = " + JSONUtil.toJsonStr(clickhouseDeployDTO));
    }

    @Test
    public void testClickhouseCasterCapacityTemplate() {
        ClickhouseDeployDTO clickhouseDeployDTO = clickhouseService.buildScaleDeployDTO(2608L, "clickhouse-stable", Arrays.asList(2,0,2,0,3));
        System.out.println("clickhouse2 = " + JSONUtil.toJsonStr(clickhouseDeployDTO));
    }

    @Test
    public void testClickhouseCasterIterationTemplate() {
        ClickhouseDeployDTO clickhouseDeployDTO = clickhouseService.buildIterationDeployDTO(2608L, "clickhouse-stable04", Arrays.asList("s-test02-01-0", "chi-olap-online-test02-04-0"));
        System.out.println("clickhouse3 = " + JSONUtil.toJsonStr(clickhouseDeployDTO));
    }
}
