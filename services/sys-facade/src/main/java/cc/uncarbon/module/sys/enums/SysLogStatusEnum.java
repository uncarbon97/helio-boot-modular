package cc.uncarbon.module.sys.enums;

import cc.uncarbon.framework.helium.base.enums.BaseEnum;
import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;


/**
 * 预置系统登录日志结果状态枚举
 * 适用于操作日志、登录日志
 */
@AllArgsConstructor
@Getter
public enum SysLogStatusEnum implements BaseEnum<Integer> {

    NON_EXECUTION(0, "未执行"),
    SUCCESS(1, "成功"),
    FAILED(2, "失败"),

    ;@EnumValue
    private final Integer value;
    private final String label;

}
