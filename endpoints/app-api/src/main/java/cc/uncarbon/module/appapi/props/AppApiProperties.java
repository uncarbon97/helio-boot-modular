package cc.uncarbon.module.appapi.props;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * app-api 配置属性类
 *
 * @author Uncarbon
 */
@ConfigurationProperties(prefix = AppApiProperties.PREFIX)
@Data
public class AppApiProperties {

    public static final String PREFIX = "app-api";

    /**
     * 无须鉴权即可访问的路由
     * 例如行政区划、轮播图等
     */
    private List<String> noAuthPaths;

}
