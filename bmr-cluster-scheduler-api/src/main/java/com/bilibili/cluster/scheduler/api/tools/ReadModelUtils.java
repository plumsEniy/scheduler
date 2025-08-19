package com.bilibili.cluster.scheduler.api.tools;

import com.baomidou.mybatisplus.core.toolkit.Constants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.ClassPathResource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ReadModelUtils {
    public static String readModel(ClassPathResource classPathResource) {
        StringBuffer modelBuffer = null;
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(classPathResource.getInputStream(), Constants.UTF_8));
            String line = "";
            modelBuffer = new StringBuffer();
            while ((line = reader.readLine()) != null) {
                modelBuffer.append(line);
                modelBuffer.append("\n");
            }
            return modelBuffer.toString();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException("读取" + classPathResource.getPath() + "失败");
        } finally {
            IOUtils.closeQuietly(reader);
        }
    }

    public static List<String> readModelByLine(ClassPathResource classPathResource) {
        List<String> result = new ArrayList<>();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(classPathResource.getInputStream(), Constants.UTF_8));
            String line = null;
            while ((line = reader.readLine()) != null) {
                if (!StringUtils.isBlank(line.trim())) {
                    result.add(line.trim());
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException("读取" + classPathResource.getPath() + "失败");
        } finally {
            IOUtils.closeQuietly(reader);
        }
        return result;
    }
}
