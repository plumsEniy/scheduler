package com.bilibili.cluster.scheduler.api.tools;

import com.bilibili.cluster.scheduler.common.enums.template.TemplateEnum;
import freemarker.template.Configuration;
import freemarker.template.Template;

import java.io.StringWriter;

/**
 * @description:
 * @Date: 2024/6/6 17:15
 * @Author: nizhiqiang
 */
public class FreemarkUtils {
    public static Configuration freemarkerConfiguration;

    static {
        freemarkerConfiguration = new Configuration(Configuration.VERSION_2_3_31);
        try {
            freemarkerConfiguration.setClassForTemplateLoading(FreemarkUtils.class, "/template");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 填充模版
     *
     * @param templateEnum
     * @param object
     * @return
     */
    public static String fillTemplate(TemplateEnum templateEnum, Object object) {
        String templatePath = templateEnum.getPath();
        try {
            Template template = freemarkerConfiguration.getTemplate(templatePath);
            StringWriter stringWriter = new StringWriter();
            template.process(object, stringWriter);
            return stringWriter.toString();
        } catch (Exception e) {
            throw new RuntimeException(String.format("fill template error, error is %s", e.getMessage()));
        }
    }

}
