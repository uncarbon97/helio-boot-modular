package cc.uncarbon.module.sys.model.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;


/**
 * 系统用户-后台管理-批量导入结果
 * <p>文件级 all-or-nothing：任一行校验失败则整文件不入库，失败行数等于总行数</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class SysUserImportResult implements Serializable {

    @Schema(description = "文件总行数（不含表头）")
    private int totalRows;

    @Schema(description = "成功导入行数")
    private int successCount;

    @Schema(description = "失败行数（= 总行数 - 成功行数）")
    private int failedCount;

    @Schema(description = "行级错误明细（至多100条）")
    private List<RowError> errors;

    @Schema(description = "错误明细是否被截断")
    private boolean errorTruncated;

    @Schema(description = "成功导入的用户IDs")
    private List<Long> importedUserIds;


    /**
     * 行级错误
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RowError implements Serializable {

        @Schema(description = "Excel 行号（1-based，含表头行）")
        private int rowNumber;

        @Schema(description = "该行账号（可能为空）")
        private String pin;

        @Schema(description = "失败原因")
        private String reason;

    }

}
