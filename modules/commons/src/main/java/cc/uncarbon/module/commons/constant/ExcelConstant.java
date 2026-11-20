package cc.uncarbon.module.commons.constant;

import lombok.experimental.UtilityClass;

/**
 * 与 Excel 导入导出相关的常量
 *
 * @author Uncarbon
 */
@UtilityClass
public class ExcelConstant {

    /**
     * xlsx 文件名后缀
     */
    public static final String XLSX_SUFFIX = "xlsx";

    /**
     * xlsx MIME 类型
     */
    public static final String XLSX_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    /**
     * 批量导入单文件数据行默认上限
     */
    public static final int IMPORT_ROWS_MAX = 1000;

    /**
     * 导入错误明细返回默认上限
     */
    public static final int IMPORT_ERRORS_MAX = 100;

}
