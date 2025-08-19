import cn.hutool.json.JSONUtil;
import com.bilibili.cluster.scheduler.api.ApiApplicationServer;
import com.bilibili.cluster.scheduler.common.dto.metric.dto.MetricNodeInstance;
import lombok.extern.slf4j.Slf4j;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @description:
 * @Date: 2024/9/18 18:02
 * @Author: nizhiqiang
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = ApiApplicationServer.class)
@Slf4j
public class CompareTest {

    public static void main(String[] args) {
        String s1 = "{\"enable_scrape\":true,\"type\":\"servers\",\"target\":\"10.155.10.32\",\"labels\":{\"app\":\"jscs-spark-ess-master-moni\",\"cluster\":\"jscs-spark-ess-master-moni\",\"product\":\"datacenter\",\"label\":\"NaN\",\"env\":\"prod\",\"zone\":\"sh001\",\"host\":\"jscs-bigdata-kyuubi-01\",\"namespace\":\"\"},\"port\":7031,\"name\":\"jscs-bigdata-kyuubi-01\"}";
        String s2 = "{\"enable_scrape\":true,\"type\":\"servers\",\"target\":\"10.155.10.32\",\"labels\":{\"app\":\"jscs-spark-ess-master-moni\",\"cluster\":\"jscs-spark-ess-master-moni\",\"label\":\"NaN\",\"env\":\"prod\",\"zone\":\"sh001\",\"namespace\":\"\",\"host\":\"jscs-bigdata-kyuubi-01\"},\"port\":7031,\"name\":\"jscs-bigdata-kyuubi-01\"}";

        MetricNodeInstance instance1 = JSONUtil.toBean(s1, MetricNodeInstance.class);
        MetricNodeInstance instance2 = JSONUtil.toBean(s2, MetricNodeInstance.class);

        System.out.println("s1.equals(s2) = " + instance1.equals(instance2));
    }
}
