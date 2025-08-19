package com.bilibili.cluster.scheduler.api.tools;

import org.springframework.core.io.ClassPathResource;

import java.util.List;
import java.util.regex.Pattern;

public class BlackListFilter {
    private static List<String> blackListRegExp;

    static {
        ClassPathResource blackListClassPath = new ClassPathResource("blackList/BlackList.txt");
        blackListRegExp = ReadModelUtils.readModelByLine(blackListClassPath);
    }

    public static boolean isJobInBlackList(String jobName) {
        for (String regExp : blackListRegExp) {
            boolean match = Pattern.matches(regExp, jobName);
            if (match) return true;
        }
        return false;
    }
}
