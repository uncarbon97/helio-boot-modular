package cc.uncarbon.module.sys.model.valueobj;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * {@link IPLocationBO} 单元测试
 */
class IPLocationBOTest {

    /**
     * unknown() 工厂应返回未知属地
     */
    @Test
    void unknownFactoryReturnsUnknownLocation() {
        Assertions.assertEquals("未知", IPLocationBO.unknown().getLocation());
    }

    /**
     * intranet() 工厂应返回内网属地
     */
    @Test
    void intranetFactoryReturnsIntranetLocation() {
        Assertions.assertEquals("内网", IPLocationBO.intranet().getLocation());
    }

    /**
     * inChina() 工厂应返回中国内地属地
     */
    @Test
    void inChinaFactoryReturnsChinaLocation() {
        Assertions.assertEquals("中国", IPLocationBO.inChina().getLocation());
    }

    /**
     * 全参构造器与取值方法应配套
     */
    @Test
    void allArgsConstructorAndGetter() {
        IPLocationBO bo = new IPLocationBO("广东省深圳市");

        Assertions.assertEquals("广东省深圳市", bo.getLocation());
    }

    /**
     * 无参构造器应支持链式 setter
     */
    @Test
    void noArgsConstructorAndChainSetter() {
        IPLocationBO bo = new IPLocationBO().setLocation("北京市");

        Assertions.assertEquals("北京市", bo.getLocation());
    }

    /**
     * Lombok equals/hashCode 应按 location 字段比较
     */
    @Test
    void lombokEqualsAndHashCode() {
        IPLocationBO left = IPLocationBO.unknown();
        IPLocationBO right = new IPLocationBO("未知");

        Assertions.assertEquals(left, right);
        Assertions.assertEquals(left.hashCode(), right.hashCode());
        Assertions.assertNotEquals(left, IPLocationBO.intranet());
        Assertions.assertNotEquals(left, null);
        Assertions.assertNotEquals(left, new Object());
    }
}
