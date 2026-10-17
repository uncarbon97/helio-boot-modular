package cc.uncarbon.module.sys.resolver;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.lionsoul.ip2region.service.Ip2Region;
import org.lionsoul.ip2region.xdb.InetAddressException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

/**
 * {@link Ip2regionBasedIPLocationResolver} 内网短路、非法入参兜底与查询异常降级测试
 */
@ExtendWith(MockitoExtension.class)
class Ip2regionBasedIPLocationResolverTest {

    @Mock
    private Ip2Region ip2Region;

    @InjectMocks
    private Ip2regionBasedIPLocationResolver resolver;


    @Test
    void innerIpShortCircuitsWithoutQuery() throws Exception {
        String ret = resolver.resolve("192.168.1.1");

        // 内网地址直接返回，不触发 ip2region 查询
        Assertions.assertEquals("内网", ret);
        Mockito.verifyNoInteractions(ip2Region);
    }

    @Test
    void publicIpDelegatesToIp2region() throws Exception {
        Mockito.when(ip2Region.search("8.8.8.8")).thenReturn("中国|0|北京市|北京市|联通");

        Assertions.assertEquals("中国|0|北京市|北京市|联通", resolver.resolve("8.8.8.8"));
        Mockito.verify(ip2Region).search("8.8.8.8");
    }

    @Test
    void nonIpv4TextFallsBackToIp2region() throws Exception {
        // 非 IPv4 文本使 Ipv4Util.isInnerIP 抛出 IllegalArgumentException，被吞掉后仍走 ip2region
        Mockito.when(ip2Region.search("not-an-ip")).thenReturn("未知区域");

        Assertions.assertEquals("未知区域", resolver.resolve("not-an-ip"));
        Mockito.verify(ip2Region).search("not-an-ip");
    }

    @Test
    void searchFailureDegradesToUnknown() throws Exception {
        Mockito.when(ip2Region.search("8.8.8.8")).thenThrow(new InetAddressException("bad ip"));

        // ip2region 查询异常 → 降级返回"未知"
        Assertions.assertEquals("未知", resolver.resolve("8.8.8.8"));
    }

    @Test
    void ioFailureDegradesToUnknown() throws Exception {
        Mockito.when(ip2Region.search("8.8.8.8")).thenThrow(new IOException("xdb broken"));

        // IO 异常同样命中多 catch 降级分支
        Assertions.assertEquals("未知", resolver.resolve("8.8.8.8"));
    }
}
