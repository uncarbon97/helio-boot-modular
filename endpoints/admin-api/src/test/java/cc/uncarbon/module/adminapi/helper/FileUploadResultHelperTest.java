package cc.uncarbon.module.adminapi.helper;

import cc.uncarbon.framework.helium.tenant.context.SimpleTenantContext;
import cc.uncarbon.framework.helium.tenant.context.TenantContextHolder;
import cc.uncarbon.module.adminapi.props.HashidsProperties;
import cc.uncarbon.module.file.model.valueobj.FileMetaDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * {@link FileUploadResultHelper} 上传结果转换分支测试（直链/代理下载路由）
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FileUploadResultHelperTest {

    private final FileUploadResultHelper helper = new FileUploadResultHelper(
            new HashidsHelper(new HashidsProperties().setSalt("test-salt-value").setMinLength(8)));

    @Mock
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        Mockito.when(request.getRequestURL())
                .thenReturn(new StringBuffer("http://localhost:8080/admin/v1/file/upload"));
    }

    @Test
    void directUrlPreferredWhenPresent() {
        var source = meta(123456789L, "https://cdn.example.com/a/b.png");

        var ret = helper.toUploadResult(source, request);

        Assertions.assertEquals("https://cdn.example.com/a/b.png", ret.getUrl());
        Assertions.assertEquals("a.png", ret.getFilename());
        Assertions.assertEquals("origin.png", ret.getOriginalFilename());
        Assertions.assertEquals(helper.getHashidsHelper().encode(123456789L), ret.getOutFileId());
    }

    @Test
    void proxyDownloadUrlBuiltWithTenantCode() {
        var source = meta(123456789L, null);

        TenantContextHolder.runWithContext(new SimpleTenantContext(5L, "租户五", "t5"), () -> {
            var ret = helper.toUploadResult(source, request);

            Assertions.assertEquals(
                    "http://localhost:8080/admin/file/download/v1/"
                            + helper.getHashidsHelper().encode(123456789L) + "/t5",
                    ret.getUrl());
        });
    }

    @Test
    void proxyDownloadUrlToleratesMissingTenantCode() {
        // 无租户上下文（平台视角上传等）→ tenantCode 以空串占位
        var source = meta(42L, "");

        var ret = helper.toUploadResult(source, request);

        Assertions.assertEquals(
                "http://localhost:8080/admin/file/download/v1/" + helper.getHashidsHelper().encode(42L) + "/",
                ret.getUrl());
    }


    /*
    ----------------------------------------------------------------
                        私有方法
    ----------------------------------------------------------------
     */

    private FileMetaDTO meta(Long id, String directUrl) {
        return new FileMetaDTO()
                .setId(id)
                .setStorageFilename("a").setOriginalFilename("origin")
                .setExtendName("png")
                .setDirectUrl(directUrl);
    }
}
