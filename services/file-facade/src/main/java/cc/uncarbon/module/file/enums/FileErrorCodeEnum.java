package cc.uncarbon.module.file.enums;

import cc.uncarbon.framework.helium.base.enums.ErrorCodeEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;


/**
 * 文件管理错误码枚举
 * 错误码格式 [A][BB][CCC]
 * [BB] 本枚举内固定为 02，表示文件管理
 * [CCC] 按具体错误区分
 */
@AllArgsConstructor
@Getter
public enum FileErrorCodeEnum implements ErrorCodeEnum {

    /*
     A 开头错误码，表示一般性错误，如用户输入有误
     */
    // 很少遇到；但是如果出现了只会提示默认的「请稍后再试」，不方便排查，还是整个文案比较好
    A02001("欲上传的文件可能已被删除，请重新选择"),

    ;private final String errorMsgFriendly;

}
