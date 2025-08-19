package com.bilibili.cluster.scheduler.api.configuration;

import com.fasterxml.classmate.TypeResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@EnableSwagger2
public class SwaggerOpenApiConfig {


//    /***
//     * 构建Swagger3.0文档说明
//     * @return 返回 OpenAPI
//     */
//    @Bean
//    public OpenAPI customOpenAPI() {
//
//        // 联系人信息(contact)，构建API的联系人信息，用于描述API开发者的联系信息，包括名称、URL、邮箱等
//        // name：文档的发布者名称 url：文档发布者的网站地址，一般为企业网站 email：文档发布者的电子邮箱
//        Contact contact = new Contact()
//                .name("bmr")                             // 作者名称
//                .email("")                   // 作者邮箱
//                .url("")  // 介绍作者的URL地址
//                .extensions(new HashMap<String, Object>()); // 使用Map配置信息（如key为"name","email","url"）
//
//        // 授权许可信息(license)，用于描述API的授权许可信息，包括名称、URL等；假设当前的授权信息为Apache 2.0的开源标准
//        License license = new License()
//                .name("Apache 2.0")                         // 授权名称
//                .url("https://www.apache.org/licenses/LICENSE-2.0.html")    // 授权信息
////                .identifier("Apache-2.0")                   // 标识授权许可
//                .extensions(new HashMap<String, Object>());// 使用Map配置信息（如key为"name","url","identifier"）
//
//        //创建Api帮助文档的描述信息、联系人信息(contact)、授权许可信息(license)
//        Info info = new Info()
//                .title("Swagger3.0 (Open API) 发布子系统")      // Api接口文档标题（必填）
//                .description("发布子系统Swagger 3.0 ")     // Api接口文档描述
//                .version("1.2.1")                                  // Api接口版本
////                .termsOfService("https://example.com/")            // Api接口的服务条款地址
//                .license(license)                                  // 设置联系人信息
//                .contact(contact);                                 // 授权许可信息
//        // 返回信息
//        return new OpenAPI()
//                .openapi("3.0.1")// Open API 3.0.1(默认)
//                .info(info);       // 配置Swagger3.0描述信息
//    }


    private TypeResolver typeResolver;

    @Autowired
    public SwaggerOpenApiConfig(TypeResolver typeResolver) {
        this.typeResolver = typeResolver;
    }

    @Bean
    public Docket docket() {
        return new Docket(DocumentationType.SWAGGER_2)
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.bilibili.cluster.scheduler.api.controller"))
                .paths(PathSelectors.any())
                .build()
                .apiInfo(apiInfo()).groupName("bilibili.datacenter");
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder().title("Bmr cluster scheduler Rest Api").description("Some custom description of Api.").build();
    }

}
