package cc.uncarbon.module.sys.dal.fieldcrypto;

import cc.uncarbon.framework.helium.db.fieldcrypto.exception.FieldCryptoException;
import cc.uncarbon.framework.helium.db.fieldcrypto.field.FieldCipher;
import cc.uncarbon.framework.helium.db.fieldcrypto.field.FieldCipherContext;
import cc.uncarbon.module.sys.constant.SysUserCipherCols;
import org.apache.ibatis.type.JdbcType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * 手机号/邮箱列级 TypeHandler 与脱敏器测试：列路径绑定、密文前缀、回读解密、跨列 AAD 隔离
 */
@ExtendWith(MockitoExtension.class)
class SysUserFieldCryptoHandlersTest {

    private static final String KEY_ID = "k1";

    @Mock
    private PreparedStatement ps;
    @Mock
    private ResultSet rs;

    @BeforeAll
    static void registerCipher() {
        FieldCipherContext.register(new FieldCipher(
                "C".repeat(32).getBytes(StandardCharsets.UTF_8), List.of(KEY_ID), KEY_ID));
    }

    @AfterAll
    static void tearDown() {
        FieldCipherContext.clear();
    }


    @Test
    void columnPathsBoundToCipherCols() {
        Assertions.assertEquals(SysUserCipherCols.PHONE_COLUMN_PATH, new PhoneEncryptedTypeHandler().columnPath());
        Assertions.assertEquals(SysUserCipherCols.EMAIL_COLUMN_PATH, new EmailEncryptedTypeHandler().columnPath());
    }

    @Test
    void maskersDelegateToMaskUtil() {
        Assertions.assertEquals("189****8927", new PhoneFieldValueMasker().mask("18967038927"));
        Assertions.assertEquals("al****@example.com", new EmailFieldValueMasker().mask("alicealex@example.com"));
    }

    @Test
    void phoneRoundTripWithKeyIdPrefix() throws SQLException {
        PhoneEncryptedTypeHandler handler = new PhoneEncryptedTypeHandler();

        handler.setNonNullParameter(ps, 1, "13800001111", JdbcType.VARCHAR);
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(ps).setString(Mockito.eq(1), captor.capture());
        String blob = captor.getValue();
        Assertions.assertTrue(blob.startsWith(KEY_ID + "$"), "密文应带 keyId 前缀: " + blob);

        Mockito.when(rs.getString("phone_no")).thenReturn(blob);
        Assertions.assertEquals("13800001111", handler.getNullableResult(rs, "phone_no"));
    }

    @Test
    void crossColumnCipherRejectedByAad() throws SQLException {
        // 手机号密文换列名（AAD 列绑定不符）→ 认证失败，密文不可跨列挪用
        PhoneEncryptedTypeHandler phoneHandler = new PhoneEncryptedTypeHandler();
        phoneHandler.setNonNullParameter(ps, 1, "13800001111", JdbcType.VARCHAR);
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(ps).setString(Mockito.eq(1), captor.capture());

        EmailEncryptedTypeHandler emailHandler = new EmailEncryptedTypeHandler();
        Mockito.when(rs.getString("email")).thenReturn(captor.getValue());
        Assertions.assertThrows(FieldCryptoException.class, () -> emailHandler.getNullableResult(rs, "email"));
    }
}
