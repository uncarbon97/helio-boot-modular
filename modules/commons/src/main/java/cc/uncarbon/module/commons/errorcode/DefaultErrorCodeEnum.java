package cc.uncarbon.module.commons.errorcode;

import cc.uncarbon.framework.helium.base.errorcode.StructuredErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;


/**
 * 默认错误码枚举
 * 错误码格式 [A][BB][CCC]
 * [BB] 本枚举内固定为 {@link ErrorCodeBizGroup#UNCLASSIFIED}
 * [CCC] 具体错误代号
 */
@Getter
@AllArgsConstructor
public enum DefaultErrorCodeEnum implements StructuredErrorCode {

    /*
     A 开头错误码，表示一般性错误，如用户输入有误
     */
    A00001("重复数据"),
    A00002("不存在数据"),
    A00003("长度不得大于 {}"),

    // 以下枚举用于通用导入导出
    A00004("导入文件为空或没有数据行"),
    A00005("导入文件表头不匹配，请使用最新模板"),
    A00006("导入文件解析失败，请确认为合法的 xlsx 文件"),
    A00007("导入文件数据行数超过上限 {} 行"),
    A00008("导出数据量超过上限 {} 行，请缩小筛选范围后重试"),
    ;private final String errorMsgFriendly;

}
