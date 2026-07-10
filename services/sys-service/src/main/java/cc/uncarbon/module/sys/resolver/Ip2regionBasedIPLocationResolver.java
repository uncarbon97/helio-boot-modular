package cc.uncarbon.module.sys.resolver;

import cc.uncarbon.module.commons.iplocation.IPLocationResolver;
import cn.hutool.core.net.Ipv4Util;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.lionsoul.ip2region.service.Ip2Region;
import org.lionsoul.ip2region.xdb.InetAddressException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 基于 {@link Ip2Region} 的 IP 归属地解析
 */
@ConditionalOnBean(value = Ip2Region.class)
@RequiredArgsConstructor
@Component
@Slf4j
public class Ip2regionBasedIPLocationResolver implements IPLocationResolver {

    private final static String LOG_PREFIX = "[IP归属地解析]";

    private final Ip2Region ip2Region;


    @Override
    public String resolve(String ip) {
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
    }
}
