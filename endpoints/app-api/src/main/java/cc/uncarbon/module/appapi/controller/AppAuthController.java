package cc.uncarbon.module.appapi.controller;


import cc.uncarbon.framework.helium.web.model.response.ApiResult;
import cc.uncarbon.module.commons.constant.ApiPathPrefix;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@Tag(name = "APP-鉴权接口")
@RequestMapping(ApiPathPrefix.APP + "/v1/auth")
@RequiredArgsConstructor
@RestController
@Slf4j
public class AppAuthController {

    /*
    /app/** 开头的C端接口默认为都需要登录，放行接口请在配置文件的 helium.security.exclude-routes 中设置
    相关拦截器代码请见 CustomInterceptorConfiguration.java
     */

    @Operation(summary = "登录")
    @PostMapping("/login")
    public ApiResult<Void> login() {
        return ApiResult.success();
    }

}
