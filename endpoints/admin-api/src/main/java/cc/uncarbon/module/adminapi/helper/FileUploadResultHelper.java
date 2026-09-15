package cc.uncarbon.module.adminapi.helper;

import cc.uncarbon.framework.helium.tenant.context.TenantContextHolder;
import cc.uncarbon.module.adminapi.model.response.FileUploadResultVO;
import cc.uncarbon.module.commons.constant.ApiPathPrefix;
import cc.uncarbon.module.file.model.valueobj.FileMetaDTO;
import cn.hutool.core.text.CharSequenceUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Optional;

/**
 * 构造文件上传结果 VO
 * 复用下载路由拼接逻辑：无直链时，按【后台管理-下载文件V1】路由生成服务端代理下载 URL
 */
@RequiredArgsConstructor
@Component
public class FileUploadResultHelper {

    /**
     * 下载请求路由，方便文本替换复用
     * 增加 v1 方便增加其他的传参契约
     */
    public static final String DOWNLOAD_ROUTE_HASHIDS = "/file/download/v1/{hashIds}/{tenantCode}";

    @Getter
    private final HashidsHelper hashidsHelper;


    /**
     * 将 {@link FileMetaDTO} 转换为 {@link FileUploadResultVO}
     *
     * @param source  文件元数据
     * @param request 当前请求（用于推导站点根 URL）
     */
    public FileUploadResultVO toUploadResult(FileMetaDTO source, HttpServletRequest request) {
        String hashIds = hashidsHelper.encode(source.getId());
        FileUploadResultVO ret = new FileUploadResultVO()
                .setOutFileId(hashIds)
                .setFilename(source.getStorageFilenameFull())
                // 返回本次上传文件的原始文件名
                .setOriginalFilename(source.getOriginalFilenameFull());

        if (CharSequenceUtil.isEmpty(source.getDirectUrl())) {
            String replacement = CharSequenceUtil.replace(DOWNLOAD_ROUTE_HASHIDS, "{hashIds}", hashIds);
            replacement = CharSequenceUtil.replace(replacement, "{tenantCode}",
                    Optional.ofNullable(TenantContextHolder.getTenantCode()).orElse(""));
            // 取当前请求 URL 的 协议://主机:端口/admin 部分，拼上下载路由
            String base = UriComponentsBuilder
                    .fromUri(URI.create(request.getRequestURL().toString()))
                    .replacePath(ApiPathPrefix.ADMIN)
                    .replaceQuery(null)
                    .toUriString();
            ret.setUrl(base + replacement);
        } else {
            ret.setUrl(source.getDirectUrl());
        }
        return ret;
    }
}
