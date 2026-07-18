package cc.uncarbon.module.adminapi.controller.file;

import cc.uncarbon.framework.helium.base.exception.BusinessException;
import cc.uncarbon.framework.helium.tenant.context.TenantContextHolder;
import cc.uncarbon.framework.helium.web.model.response.ApiResult;
import cc.uncarbon.module.adminapi.helper.HashidsHelper;
import cc.uncarbon.module.adminapi.model.response.FileUploadResultVO;
import cc.uncarbon.module.commons.constant.ApiPathPrefix;
import cc.uncarbon.module.commons.satoken.StpLoginType;
import cc.uncarbon.module.file.errorcode.FileErrorCodeEnum;
import cc.uncarbon.module.file.facade.FileUpDownloadFacade;
import cc.uncarbon.module.file.model.internal.FacadeUploadOptions;
import cc.uncarbon.module.file.model.request.FileAttrExtraRequest;
import cc.uncarbon.module.file.model.response.FileDownloadReply;
import cc.uncarbon.module.file.model.valueobj.FileMetaDTO;
import cc.uncarbon.module.file.util.UploadFileChecker;
import ch.qos.logback.core.util.FileSize;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.http.Header;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;


@Tag(name = "后台管理-文件上传、下载")
@RequestMapping(value = ApiPathPrefix.ADMIN)
@RequiredArgsConstructor
@RestController
@Slf4j
public class AdminFileUpDownloadController {

    /**
     * 上传请求路由，方便文本替换复用
     */
    private static final String UPLOAD_ROUTE = "/file/upload";

    /**
     * 下载请求路由，方便文本替换复用
     * 增加 v1 方便增加其他的传参契约
     */
    private static final String DOWNLOAD_ROUTE_HASHIDS = "/file/download/v1/{hashIds}/{tenantCode}";

    private final FileUpDownloadFacade fileUpDownloadFacade;
    private final HashidsHelper hashidsHelper;

    /**
     * 从配置文件中获取的最大文件上传大小
     */
    @Value(value = "${spring.servlet.multipart.max-file-size:1024MB}")
    private String springServletMultipartMaxFileSize;


    @Operation(summary = "上传文件")
    @PostMapping(value = UPLOAD_ROUTE)
    // 约束：登录后才能上传
    @SaCheckLogin(type = StpLoginType.ADMIN)
    public ApiResult<FileUploadResultVO> upload(
            @RequestPart MultipartFile file, @RequestPart(required = false) @Valid FileAttrExtraRequest attr,
            HttpServletRequest request
    ) throws IOException {
        checkBeforeUpload(file);

        // 已存在文件，直接返回 URL
        String digest = DigestUtil.sha256Hex(file.getBytes());
        FileMetaDTO fileMeta = fileUpDownloadFacade.findByDigestSha256(digest);
        if (fileMeta == null) {
            // 不存在，走正常上传
            var options = new FacadeUploadOptions();
            options
                    .setOriginalFilename(file.getOriginalFilename())
                    .setContentType(file.getContentType())
                    .setDigestSha256(digest)
                    .setPlatform(null)
                    .setUseOriginalFilenameAsDownloadFileName(false);
            fileMeta = fileUpDownloadFacade.upload(file.getBytes(), options, attr);
        }
        return ApiResult.success(toUploadResult(fileMeta, request.getRequestURL().toString()));
    }

    @Operation(summary = "下载文件V1")
    @GetMapping(value = DOWNLOAD_ROUTE_HASHIDS)
    public void downloadV1(HttpServletResponse servletResponse,
                           @PathVariable String hashIds,
                           @Parameter(description = "租户编码，用于多租户隔离；无租户时不传",
                                   example = "91330105MACPN4X08Y")
                           @PathVariable(required = false) String tenantCode) throws IOException {
        Long id = hashidsHelper.decode(hashIds);
        FileErrorCodeEnum.A02006.throwIfNull(id);

        FileDownloadReply reply;
        if (CharSequenceUtil.isEmpty(tenantCode)) {
            tenantCode = null;
        }
        reply = fileUpDownloadFacade.downloadByTenantAndId(tenantCode, id);
        if (!reply.isSuccess()) {
            throw new BusinessException(reply.getErrorCode());
        }

        if (reply.isRedirect2DirectUrl()) {
            // 302重定向
            servletResponse.sendRedirect(reply.getDirectUrl());
            return;
        }

        // 服务端代理下载
        servletResponse.setHeader(Header.CONTENT_TYPE.getValue(), MediaType.APPLICATION_OCTET_STREAM_VALUE);
        servletResponse.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        String downFileName = URLEncoder.encode(reply.getStorageFilename(), StandardCharsets.UTF_8);
        servletResponse.setHeader(Header.CONTENT_DISPOSITION.getValue(), "attachment;filename=" + downFileName);
        // 把文件写入响应流
        IoUtil.write(servletResponse.getOutputStream(), false, reply.getFileBytes());
    }

    /*
    ----------------------------------------------------------------
                        私有方法 private methods
    ----------------------------------------------------------------
     */

    private void checkBeforeUpload(MultipartFile file) {
        FileErrorCodeEnum.A02001.throwIfNull(file);
        FileSize parsedMax = FileSize.valueOf(springServletMultipartMaxFileSize);
        var errorCodeEnum = UploadFileChecker.check(file,
                // 默认只能上传 springServletMultipartMaxFileSize 之内的文件，这里除以 1024 转换为 KB
                parsedMax.getSize() >> 10,
                // 且约束后缀名
                UploadFileChecker.COMMON_EXTEND_NAMES);
        if (errorCodeEnum != null) {
            throw new BusinessException(errorCodeEnum);
        }
    }

    /**
     * 将 {@link FileMetaDTO} 转换为 {@link FileUploadResultVO}
     */
    private FileUploadResultVO toUploadResult(@NonNull FileMetaDTO source,
                                              @NonNull String requestUrl) {
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
            ret.setUrl(CharSequenceUtil.replace(requestUrl, UPLOAD_ROUTE, replacement));
        } else {
            ret.setUrl(source.getDirectUrl());
        }
        return ret;
    }
}
