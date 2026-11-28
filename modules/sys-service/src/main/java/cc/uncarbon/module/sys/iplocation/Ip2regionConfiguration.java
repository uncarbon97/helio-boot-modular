package cc.uncarbon.module.sys.iplocation;

import cn.hutool.core.net.Ipv4Util;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.lionsoul.ip2region.service.Config;
import org.lionsoul.ip2region.service.Ip2Region;
import org.lionsoul.ip2region.xdb.InetAddressException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;

/**
 * ip2region 离线IP归属地查询
 * 需要将 xdb 文件放置于 classpath:ip2region/ 目录
 */
@Slf4j
@Configuration
@ConditionalOnExpression(value = "${sys.ip2region.enabled:false}")
public class Ip2regionConfiguration {

    private final static String LOG_PREFIX = "[IP归属地解析]";


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

    @Bean
    @ConditionalOnMissingBean
    public IPLocationResolver ip2regionBasedIPLocationResolver(Ip2Region ip2Region) {
        return (ip -> {
            try {
                if (Ipv4Util.isInnerIP(ip)) {
                    return "内网";
                }
            } catch (IllegalArgumentException _) {
                // 有可能不是 IPv4 地址
            }
            try {
                return ip2Region.search(ip);
            } catch (InetAddressException | IOException | InterruptedException e) {
                log.warn(LOG_PREFIX + "[IP2region] 解析失败 >> ip={}", ip);
                return "未知";
            }
        });
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
