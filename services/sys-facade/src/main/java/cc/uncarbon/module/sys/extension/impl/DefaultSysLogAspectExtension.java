package cc.uncarbon.module.sys.extension.impl;

import cc.uncarbon.module.sys.extension.SysLogAspectExtension;
import cc.uncarbon.module.sys.model.response.IPLocationBO;
import cn.hutool.core.net.NetUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.text.StrPool;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DefaultSysLogAspectExtension implements SysLogAspectExtension {

    @Override
    public IPLocationBO queryIPLocation(String ip) {
        if (CharSequenceUtil.contains(ip, StrPool.COLON)) {
            // 仅根据冒号简易判断；暂不支持IPv6地址
            return IPLocationBO.unknown();
        }

        if (NetUtil.isInnerIP(ip)) {
            return IPLocationBO.intranet();
        }

        // 该API主要支持中国内地
        HttpRequest httpRequest = HttpRequest.get("http://whois.pconline.com.cn/ipJson.jsp")
                .form("ip", ip)
                .form("json", Boolean.TRUE.toString())
                .charset(CharsetUtil.CHARSET_GBK)
                // since 1.11.0，加个UA避免被当成恶意请求，造成查IP失败
                .header(Header.USER_AGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36")
                .timeout(5000);
        try (HttpResponse httpResponse = httpRequest.execute()) {
            String repStr = httpResponse.body();
            if (JSONUtil.isTypeJSONObject(repStr)) {
                JSONObject repJson = JSONUtil.parseObj(repStr);
                String pro = repJson.getStr("pro");
                String err = repJson.getStr("err");
                if (CharSequenceUtil.isEmpty(pro) || "noprovince".equals(err)) {
                    // 可能是非中国内地IP
                    String addr = repJson.getStr("addr");
                    return new IPLocationBO(addr);
                }
                // 拼接省份+城市
                String location = pro + repJson.getStr("city");
                return new IPLocationBO(location);
            }
        } catch (Exception e) {
            log.error("[SysLog切面][查询IP地址属地异常] >> ip={}  \n", ip, e);
        }

        return IPLocationBO.unknown();
    }
}
