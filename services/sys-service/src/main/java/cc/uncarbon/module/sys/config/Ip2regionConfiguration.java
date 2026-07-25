package cc.uncarbon.module.sys.config;

import lombok.SneakyThrows;
import org.lionsoul.ip2region.service.Config;
import org.lionsoul.ip2region.service.Ip2Region;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;

// @Configuration
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
        try (InputStream is = resource.getInputStream()) {
            v4Config = Config.custom()
                    .setCachePolicy(Config.BufferCache)     // 指定缓存策略:  NoCache / VIndexCache / BufferCache
                    .setSearchers(15)                       // 设置初始化的查询器数量
                    .setXdbInputStream(is)
                    // .setCacheSliceBytes(int)             // 设置缓存的分片字节数，默认为 50MiB
                    .asV4();    // 指定为 v4 配置
        }

        resource = new ClassPathResource("ip2region/ip2region_v6.xdb");
        try (InputStream is = resource.getInputStream()) {
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
}
