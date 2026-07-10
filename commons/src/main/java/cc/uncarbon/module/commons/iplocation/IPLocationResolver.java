package cc.uncarbon.module.commons.iplocation;

/**
 * IP 地址归属地解析器
 */
public interface IPLocationResolver {

    /**
     * 解析 IP 地址归属地
     * @return 内网地址返回"内网"；公网地址返回形如"中国|北京市|北京市|东城区"
     */
    String resolve(String ip);

}
