package cc.uncarbon.module.sys.constant;

/**
 * sys_user 加密相关列与密钥用途常量
 *
 * <p>wrapper 条件只准引用本类常量（静态规则 {@code SysUserCipherColsGuardTest} 封口）；
 * purpose/aad 为冻结常量：purpose 变更 = 索引全量重建，AAD 变更 = 全量重加密事故。</p>
 *
 * @author Uncarbon
 */
public final class SysUserCipherCols {

    /*
     * 盲索引用途（与 SysUserEntity @BlindIndexOf 声明一致，guard 测试反射断言同源）
     */
    public static final String PHONE_FULL_PURPOSE = "sys_user.phone_no.full";
    public static final String PHONE_TAIL_PURPOSE = "sys_user.phone_no.tail8";
    public static final String EMAIL_FULL_PURPOSE = "sys_user.email.full";

    /*
     * GCM AAD（与实体注解 / TypeHandler columnAad() 一致，永久冻结）
     */
    public static final String PHONE_CIPHER_AAD = "sys_user.phone_no_encrypted";
    public static final String EMAIL_CIPHER_AAD = "sys_user.email_encrypted";

    /*
     * DB 列名
     */
    public static final String PHONE_FULL_INDEX_COLUMN = "phone_no_blind_index";
    public static final String PHONE_TAIL_INDEX_COLUMN = "phone_no_tail_blind_index";
    public static final String EMAIL_FULL_INDEX_COLUMN = "email_blind_index";
    public static final String PHONE_CIPHER_COLUMN = "phone_no_encrypted";
    public static final String EMAIL_CIPHER_COLUMN = "email_encrypted";
    public static final String PHONE_MASKED_COLUMN = "phone_no_masked";
    public static final String EMAIL_MASKED_COLUMN = "email_masked";

    private SysUserCipherCols() {
    }
}
