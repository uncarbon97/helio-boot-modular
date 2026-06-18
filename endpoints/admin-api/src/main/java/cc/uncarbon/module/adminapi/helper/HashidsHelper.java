package cc.uncarbon.module.adminapi.helper;

import cc.uncarbon.module.adminapi.props.HashidsProperties;
import cn.hutool.core.codec.Hashids;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

@EnableConfigurationProperties(value = HashidsProperties.class)
@Component
public class HashidsHelper {

    /**
     * 自定义字母表，去掉 0, O, 1, l, I 等易混淆字符
     */
    private static final char[] CUSTOM_ALPHABET_ARRAY = {
            'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h',
            'i', 'j', 'k', 'm', 'n', 'p', 'q', 'r',
            's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
            '2', '3', '4', '5', '6', '7', '8', '9',
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H',
            'J', 'K', 'L', 'M', 'N', 'P', 'Q', 'R',
            'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'
    };

    private final Hashids hashids;

    public HashidsHelper(HashidsProperties props) {
        this.hashids = new Hashids(props.getSalt().toCharArray(), CUSTOM_ALPHABET_ARRAY, props.getMinLength());
    }

    public String encode(Long num) {
        return hashids.encode(num);
    }

    public Long decode(String str) {
        try {
            long[] arr = hashids.decode(str);
            return arr.length > 0 ? arr[0] : null;
        } catch (Exception e) {
            return null;
        }
    }
}