import cn.hutool.core.date.DateUtil;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.utils.LocalDateFormatterUtils;
import org.junit.Test;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class TestUnit {
    public static void main(String[] args) {
        String aa = "1700557595341";
        Long aLong = Long.valueOf(aa);
        Date date = new Date(aLong);
        String format = DateUtil.format(date, "yyyy-MM-dd HH:mm:ss");
        System.out.println(format);
    }


    @org.junit.Test
    public void getDate() {
        String date = DateUtil.formatDate(new Date(System.currentTimeMillis() - TimeUnit.HOURS.toMillis(20)));
        System.out.println(date);
    }

    @Test
    public void getTime() {
        System.out.println(getSparkDeployExecTime(LocalDateTime.now()));
        System.out.println(getSparkDeployExecTime(LocalDateTime.now().plusHours(12)));
    }

    private long getSparkDeployExecTime(LocalDateTime dateTime) {
        final int hour = dateTime.getHour();
        // 夜间跳过封网审批
        if (hour >= 23 || hour <= 8) {
            return LocalDateFormatterUtils.parseByPattern(Constants.FMT_DATE_TIME,
                    "2025-06-26 12:00:00").toEpochSecond(ZoneOffset.of("+08"));
        } else {
            return dateTime.toEpochSecond(ZoneOffset.of("+08"));
        }
    }
}
