package cc.uncarbon.module.sys.config;

import lombok.SneakyThrows;
import org.lionsoul.ip2region.service.Config;
import org.lionsoul.ip2region.service.Ip2Region;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;

/**
 * ip2region 离线IP归属地查询
 * 需要将 xdb 文件放置于 classpath:ip2region/ 目录
 *
 * @see cc.uncarbon.module.sys.resolver.Ip2regionBasedIPLocationResolver
 */
@ConditionalOnExpression(value = "${sys.ip2region.enabled:false}")
@Configuration
public class Ip2regionConfiguration {

    /**
     * 优先级：XdbInputStream -> XdbFile -> XdbPath
     * setXdbInputStream 仅方便使用者从 jar 包中加载 xdb 文件内容，这时 cachePolicy 只能设置为 Config.BufferCache
     */
    @SneakyThrows
    @Bean
    public Ip2Region ip2Region() {
        Config v4Config, v6Config;
        ClassPathResource resource = new ClassPathResource("ip2region/ip2region_v4.xdb");
        try (InputStream is = loadXdb(resource)) {
            v4Config = Config.custom()
                    .setCachePolicy(Config.BufferCache)     // 指定缓存策略:  NoCache / VIndexCache / BufferCache
                    .setSearchers(15)                       // 设置初始化的查询器数量
                    .setXdbInputStream(is)
                    // .setCacheSliceBytes(int)             // 设置缓存的分片字节数，默认为 50MiB
                    .asV4();    // 指定为 v4 配置
        }

        resource = new ClassPathResource("ip2region/ip2region_v6.xdb");
        try (InputStream is = loadXdb(resource)) {
            v6Config = Config.custom()
                    .setCachePolicy(Config.BufferCache)     // 指定缓存策略:  NoCache / VIndexCache / BufferCache
                    .setSearchers(15)                       // 设置初始化的查询器数量
                    .setXdbInputStream(is)
                    // .setCacheSliceBytes(int)             // 设置缓存的分片字节数，默认为 50MiB
                    .asV6();    // 指定为 v6 配置
        }
        Ip2Region ret = Ip2Region.create(v4Config, v6Config);
        // 快速验证是否可用，如果不可用会启动失败
        ret.search("1.1.1.1");
        return ret;
    }

    private static InputStream loadXdb(ClassPathResource resource) throws IOException {
        if (!resource.exists()) {
            throw new IllegalStateException(
                    "已开启 sys.ip2region.enabled，但未找到 " + resource.getPath()
                            + " ；请将 xdb 文件放置于 classpath:ip2region/ 目录，或关闭该开关"
            );
        }
        return resource.getInputStream();
    }
}
