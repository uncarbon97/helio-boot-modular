package cc.uncarbon.module.sys.resolver;

import cc.uncarbon.module.commons.resoler.IPLocationResolver;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.lionsoul.ip2region.xdb.Searcher;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Slf4j
@Component
public class Ip2regionBasedIPLocationResolver implements IPLocationResolver {

    private volatile byte[] cBuff;

    @PostConstruct
    public void init() {
        // TODO ip2region 可能还有更高版本
        try {
            ClassPathResource resource = new ClassPathResource("ip2region.xdb");
            try (InputStream is = resource.getInputStream()) {
                cBuff = is.readAllBytes();
            }
            log.info("ip2region.xdb loaded, size={}", cBuff.length);
        } catch (Exception e) {
            log.warn("ip2region.xdb not found on classpath, IP location resolution disabled");
        }
    }

    @Override
    public String resolve(String ip) {
        if (ip == null || ip.isBlank() || cBuff == null) {
            return "";
        }
        // Searcher is not thread-safe, create per call; cBuff is safe to share
        try (Searcher searcher = Searcher.newWithBuffer(cBuff)) {
            String region = searcher.search(ip);
            return region != null ? region : "";
        } catch (Exception e) {
            log.debug("IP location lookup failed for {}: {}", ip, e.getMessage());
            return "";
        }
    }
}
