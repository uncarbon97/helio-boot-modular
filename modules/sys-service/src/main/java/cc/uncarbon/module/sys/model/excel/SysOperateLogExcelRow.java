package cc.uncarbon.module.sys.model.excel;

import cc.uncarbon.framework.helium.excel.model.ExcelRow;
import cc.uncarbon.framework.helium.excel.util.ExcelValueUtil;
import cc.uncarbon.framework.helium.i18n.context.I18nContext;
import cc.uncarbon.framework.helium.i18n.context.I18nContextHolder;
import cc.uncarbon.framework.helium.i18n.context.timezone.TimezoneInfo;
import cc.uncarbon.module.sys.model.valueobj.SysOperateLogDTO;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DatePattern;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.apache.fesod.sheet.annotation.ExcelIgnoreUnannotated;
import org.apache.fesod.sheet.annotation.ExcelProperty;

import java.io.Serializable;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 系统操作日志
 */
@Data
@NoArgsConstructor
@Accessors(chain = true)
@ExcelIgnoreUnannotated
public class SysOperateLogExcelRow implements ExcelRow, Serializable {

    @ExcelProperty("操作时间")
    private String createdAt;

    @ExcelProperty("业务类型")
    private String bizType;

    @ExcelProperty("行为")
    private String behavior;

    @ExcelProperty("业务号")
    private String bizNo;

    @ExcelProperty("操作内容")
    private String operation;

    @ExcelProperty("额外业务信息")
    private String bizExtra;

    @ExcelProperty("操作人ID")
    private Long userId;

    @ExcelProperty("操作人昵称")
    private String userNickname;

    @ExcelProperty("用户类型")
    private String userType;

    @ExcelProperty("请求方法")
    private String requestMethod;

    @ExcelProperty("请求路径")
    private String requestPath;

    @ExcelProperty("来源IP")
    private String visitorIp;

    @ExcelProperty("IP归属地")
    private String visitorIpLocation;

    @ExcelProperty("结果状态")
    private String resultStatus;

    @ExcelProperty("失败原因")
    private String failedMsg;

    @ExcelProperty("来源UA")
    private String visitorUserAgent;

    /**
     * DTO 转 ExcelRow
     *
     * @param includeUserAgent 是否导出来源UA列（false 时置空）
     */
    public static SysOperateLogExcelRow of(SysOperateLogDTO dto, boolean includeUserAgent) {
        final DateTimeFormatter instantFormatter = DatePattern.NORM_DATETIME_FORMATTER.withZone(
                I18nContextHolder.getContextOptional().map(I18nContext::getTimezoneInfo).map(TimezoneInfo::getZoneId)
                        .orElseGet(ZoneId::systemDefault));

        SysOperateLogExcelRow row = new SysOperateLogExcelRow();
        BeanUtil.copyProperties(dto, row, ExcelValueUtil.defaultCopyOptions(instantFormatter));
        // 按需改写字段
        row.setUserType(ExcelValueUtil.labelOf(dto.getUserTypeCode()));
        if (!includeUserAgent) {
            row.setVisitorUserAgent(null);
        }
        return row;
    }
}
