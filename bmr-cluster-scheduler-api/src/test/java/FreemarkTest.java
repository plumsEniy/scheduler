import com.bilibili.cluster.scheduler.api.ApiApplicationServer;
import com.bilibili.cluster.scheduler.api.service.presto.PrestoService;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

/**
 * @description: freemark测试
 * @Date: 2024/6/7 11:34
 * @Author: nizhiqiang
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = ApiApplicationServer.class)
@Slf4j
public class FreemarkTest {

    @Resource
    PrestoService prestoService;

    @Test
    public void testPrestoFreemarkTemplate() {
        String yaml = prestoService.queryPrestoTemplate(50L, 1793L, "test");
        System.out.println("yaml = " + yaml);
    }

}
