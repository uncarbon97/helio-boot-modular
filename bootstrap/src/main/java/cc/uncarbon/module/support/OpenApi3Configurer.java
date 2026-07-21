package cc.uncarbon.module.support;

import cc.uncarbon.framework.helium.openapi.autoconfigure.HeliumOpenApi3AutoConfiguration;
import cn.hutool.core.collection.CollUtil;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAPI 3 增强配置
 *
 * @author Uncarbon
 * @author 芋道源码@yudao
 */
@AutoConfigureBefore(value = HeliumOpenApi3AutoConfiguration.class)
@Configuration
public class OpenApi3Configurer {


    @Bean
    public OpenAPI openAPI() {
        Map<String, SecurityScheme> securitySchemes = buildSecuritySchemes();
        List<SecurityRequirement> securityRequirements = securitySchemes.keySet().stream()
                .map(headerName -> new SecurityRequirement().addList(headerName))
                .toList();

        OpenAPI openAPI = new OpenAPI();
        // 全局安全认证
        openAPI.components(new Components().securitySchemes(securitySchemes));
        securityRequirements.forEach(openAPI::addSecurityItem);
        return openAPI;
    }

    /**
     * 除了全局安全认证，在 SpringDoc 扫描完之后，给每个接口挨个另外添加安全认证
     * 如此可以实现全局安全认证和接口安全认证的共存
     *
     * @see <a href="https://gitee.com/xiaoym/knife4j/issues/I69QBU">...</a>
     */
    @Bean
    public GlobalOpenApiCustomizer securityGlobalOpenApiCustomizer() {
        // 开销不大，重新 new 一遍无伤大雅
        Map<String, SecurityScheme> securitySchemes = buildSecuritySchemes();
        List<SecurityRequirement> securityRequirements = securitySchemes.keySet().stream()
                .map(headerName -> new SecurityRequirement().addList(headerName))
                .toList();

        return openApi -> {
            if (CollUtil.isNotEmpty(openApi.getPaths())) {
                openApi.getPaths().forEach((_, pathItem) -> {
                    pathItem.readOperations().forEach(operation -> {
                        securityRequirements.forEach(operation::addSecurityItem);
                    });
                });
            }
        };
    }

    /*
    ----------------------------------------------------------------
                        私有方法 private methods
    ----------------------------------------------------------------
     */

    private static Map<String, SecurityScheme> buildSecuritySchemes() {
        Map<String, SecurityScheme> securitySchemes = new HashMap<>(1, 1);

        String headerName = HttpHeaders.AUTHORIZATION;
        SecurityScheme securityScheme = new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .name(headerName)
                .in(SecurityScheme.In.HEADER);
        securitySchemes.put(headerName, securityScheme);
        return securitySchemes;
    }
}
