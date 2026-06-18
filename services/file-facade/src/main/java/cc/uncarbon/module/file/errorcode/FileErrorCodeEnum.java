package cc.uncarbon.module.file.errorcode;

import cc.uncarbon.framework.helium.base.errorcode.StructuredErrorCode;
import cc.uncarbon.module.commons.errorcode.ErrorCodeBizGroup;
import lombok.AllArgsConstructor;
import lombok.Getter;


/**
 * 文件管理错误码枚举
 * 错误码格式 [A][BB][CCC]
 * [BB] 本枚举内固定为 {@link ErrorCodeBizGroup#FILE}
 * [CCC] 具体错误代号
 */
@AllArgsConstructor
@Getter
public enum FileErrorCodeEnum implements StructuredErrorCode {

    /**
     * 特殊错误码，表示成功
     */
    OK("OK"),

    /*
     A 开头错误码，表示一般性错误，如用户输入有误
     */
    // 很少遇到；但是如果出现了只会提示默认的「请稍后再试」，不方便排查，还是整个文案比较好
    A02001("文件不存在，请重新选择"),
    A02002("上传的文件个数超出限制"),
    A02003("上传的文件大小超出限制"),
    A02004("上传的文件类型超出限制"),
    A02005("上传的文件不能为空"),
    A02006("文件不存在或无权限访问"),

    /*
     B 开头错误码，表示本服务内部错误
     */
    B02001( "文件上传失败，请联系管理员"),
    B02002( "文件下载失败，请联系管理员"),
    B02003("存储点 {} 不存在或缺少详细配置"),

    ;private final String errorMsgFriendly;

}
