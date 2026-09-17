package cc.uncarbon.module.sys.enums;

import cc.uncarbon.framework.helium.base.enums.BaseEnum;
import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;


/**
 * 日志结果状态枚举
 */
@AllArgsConstructor
@Getter
public enum LogResultStatusEnum implements BaseEnum<Integer> {

    SUCCESS(200, "成功"),
    FAILED(400, "失败"),

    ;@EnumValue
    private final Integer value;
    private final String label;

}
