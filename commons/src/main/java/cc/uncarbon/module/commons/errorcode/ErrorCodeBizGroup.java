package cc.uncarbon.module.commons.errorcode;

import cc.uncarbon.framework.helium.base.errorcode.StructuredErrorCode;
import lombok.experimental.UtilityClass;

/**
 * 本常量用于归类 {@link StructuredErrorCode} 的 [BB] 段业务域
 *
 * @author Uncarbon
 */
@UtilityClass
public class ErrorCodeBizGroup {

    /**
     * 未分类 / 不区分业务域
     */
    String UNCLASSIFIED = "00";

    /**
     * 系统管理
     */
    String SYS = "01";

    /**
     * 文件管理
     */
    String FILE = "02";

    /**
     * 租户管理
     */
    String TENANT = "03";

    /**
     * 后台管理 API 端点
     */
    String ADMIN_API = "04";

}
