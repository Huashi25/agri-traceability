package com.agri.traceability.config;

import com.agri.traceability.common.JwtInterceptor;
import com.agri.traceability.common.RoleInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * Web 层统一配置：注册登录/权限拦截器，以及上传文件的静态资源映射。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private JwtInterceptor jwtInterceptor;

    @Autowired
    private RoleInterceptor roleInterceptor;

    /**
     * 上传文件落盘目录。
     * 与 FileUploadController 共用 application.yml 的 file.upload-path，
     * 避免出现"文件传进去了、但浏览器访问 404"的路径不一致问题。
     */
    @Value("${file.upload-path}")
    private String uploadPath;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/user/login",
                        "/api/user/register",
                        "/doc.html",
                        "/webjars/**",
                        "/swagger-resources/**",
                        "/v2/api-docs",
                        "/v2/api-docs-ext",
                        "/swagger-ui.html",
                        "/swagger-resources/configuration/ui",
                        "/swagger-resources/configuration/security"
                );

        registry.addInterceptor(roleInterceptor)
                .addPathPatterns("/api/**");
    }

    /**
     * 把 /uploads/** 映射到上传目录。
     * 配置项可能是相对路径（如 ../uploads），这里统一转成绝对路径再注册，
     * 否则 Spring 会相对进程工作目录解析，容易出现找不到目录的情况。
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Windows 下转出的路径含反斜杠，统一换成正斜杠，拼成标准的 file: URL
        String absolutePath = Paths.get(uploadPath)
                .toAbsolutePath()
                .normalize()
                .toString()
                .replace("\\", "/");
        if (!absolutePath.endsWith("/")) {
            absolutePath = absolutePath + "/";
        }
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + absolutePath);
    }
}
