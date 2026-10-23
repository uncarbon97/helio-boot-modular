package cc.uncarbon.module.sys.constant;

import lombok.experimental.UtilityClass;

/**
 * sys_user 敏感字段加密相关常量
 * <P>本常量值一经确定，不得擅自变更，否则会导致：数据无法解密，或盲索引失效
 *
 * @author Uncarbon
 */
@UtilityClass
public class SysUserCipherCols {

    private static final String PREFIX = "sys_user.";

    /*
     * 列路径
     */
    public static final String PHONE_COLUMN_PATH = PREFIX + "phone_no";
    public static final String EMAIL_COLUMN_PATH = PREFIX + "email";

    /*
     * 盲索引用途
     */
    public static final String PHONE_FULL_PURPOSE = PREFIX + "phone_no.full";
    public static final String PHONE_TAIL4_PURPOSE = PREFIX + "phone_no.tail4";
    public static final String EMAIL_FULL_PURPOSE = PREFIX + "email.full";

}
