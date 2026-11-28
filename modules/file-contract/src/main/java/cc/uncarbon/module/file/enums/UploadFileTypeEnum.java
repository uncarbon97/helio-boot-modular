package cc.uncarbon.module.file.enums;

import cc.uncarbon.framework.helium.base.enums.BaseEnum;
import cc.uncarbon.module.commons.constant.ExcelConstant;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Predicate;


/**
 * 允许上传的文件类型枚举，同时做到「后缀名白名单 + 推导 MIME 类型 + magic-number 判断」
 */
@Getter
@AllArgsConstructor
public enum UploadFileTypeEnum implements BaseEnum<String> {

    /**
     * JPEG 图片
     */
    JPG("jpg", "JPEG 图片", "image/jpeg",
            head -> head.length >= 3
                    && (head[0] & 0xFF) == 0xFF && (head[1] & 0xFF) == 0xD8 && (head[2] & 0xFF) == 0xFF),

    /**
     * PNG 图片
     */
    PNG("png", "PNG 图片", "image/png",
            head -> head.length >= 8
                    && (head[0] & 0xFF) == 0x89 && head[1] == 0x50 && head[2] == 0x4E && head[3] == 0x47
                    && head[4] == 0x0D && head[5] == 0x0A && head[6] == 0x1A && head[7] == 0x0A),

    /**
     * WebP 图片（RIFF....WEBP 容器）
     */
    WEBP("webp", "WebP 图片", "image/webp",
            head -> head.length >= 12
                    && head[0] == 0x52 && head[1] == 0x49 && head[2] == 0x46 && head[3] == 0x46
                    && head[8] == 0x57 && head[9] == 0x45 && head[10] == 0x42 && head[11] == 0x50),

    /**
     * GIF 图片（GIF87a / GIF89a）
     */
    GIF("gif", "GIF 图片", "image/gif",
            head -> head.length >= 4
                    && head[0] == 0x47 && head[1] == 0x49 && head[2] == 0x46 && head[3] == 0x38),

    /**
     * xlsx 表格（ZIP 本地文件头 PK\x03\x04，兼容空档/跨卷签名）
     */
    XLSX("xlsx", "xlsx 表格", ExcelConstant.XLSX_CONTENT_TYPE,
            head -> head.length >= 4
                    && head[0] == 0x50 && head[1] == 0x4B
                    && ((head[2] == 0x03 && head[3] == 0x04)
                        || (head[2] == 0x05 && head[3] == 0x06)
                        || (head[2] == 0x07 && head[3] == 0x08))),

    ;private final String value;
    private final String label;

    /**
     * 推导出的 Content-Type；不信任客户端上报值，防直链存储点存储型 XSS
     */
    private final String contentType;

    /**
     * 文件头 magic-number 判断器；null=不校验
     */
    private final Predicate<byte[]> magicNumberMatcher;

    /**
     * 按小写后缀名查找
     */
    public static UploadFileTypeEnum ofExtend(String extend) {
        if (extend == null) {
            return null;
        }
        String lower = extend.toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(item -> Objects.equals(item.value, lower))
                .findFirst()
                .orElse(null);
    }

    /**
     * 全部允许的后缀名
     */
    public static String[] allowedExtends() {
        return Arrays.stream(values()).map(UploadFileTypeEnum::getValue).toArray(String[]::new);
    }

    /**
     * 校验文件头是否匹配 magic-number；未配置判断器的类型恒为通过
     *
     * @param head 文件头字节（至少截取前几个字节，可能不足）
     */
    public boolean matchesMagicNumber(byte[] head) {
        return magicNumberMatcher == null || magicNumberMatcher.test(head);
    }

}
