import cn.hutool.json.JSONUtil;
import com.bilibili.cluster.scheduler.api.ApiApplicationServer;
import com.bilibili.cluster.scheduler.api.service.dns.DnsService;
import com.bilibili.cluster.scheduler.common.dto.dns.DnsInfo;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.List;

/**
 * @description: dns测试
 * @Date: 2025/4/22 12:00
 * @Author: nizhiqiang
 */

@RunWith(SpringRunner.class)
@SpringBootTest(classes = ApiApplicationServer.class)
@Slf4j
public class DnsTest {

    @Resource
    DnsService dnsService;

    @Test
    public void testQueryDnsInfoList() {
        List<DnsInfo> dnsInfoList = dnsService.queryDnsInfoListByIp("10.155.186.8", 1, 10);
        System.out.println("dnsInfoList = " + JSONUtil.toJsonStr(dnsInfoList));
    }
}
