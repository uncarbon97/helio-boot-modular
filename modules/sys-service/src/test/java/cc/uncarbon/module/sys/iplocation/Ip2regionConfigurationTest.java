package cc.uncarbon.module.sys.iplocation;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * {@link Ip2regionConfiguration} 缺失 xdb 资源时的快速失败测试
 *
 * <p>单测 classpath 未放置 ip2region/*.xdb 离线库，仅覆盖资源缺失分支；
 * 资源存在时需要真实 xdb 文件加载，不在纯单测范围内</p>
 */
class Ip2regionConfigurationTest {

    @Test
    void ip2RegionFailsFastWhenXdbResourceMissing() {
        Ip2regionConfiguration configuration = new Ip2regionConfiguration();

        // 未放置 xdb 文件 → 启动即失败并给出补救提示
        IllegalStateException ex = Assertions.assertThrows(IllegalStateException.class, configuration::ip2Region);
        Assertions.assertTrue(ex.getMessage().contains("未找到 ip2region/ip2region_v4.xdb"));
        Assertions.assertTrue(ex.getMessage().contains("请将 xdb 文件放置于 classpath:ip2region/ 目录"));
    }
}
