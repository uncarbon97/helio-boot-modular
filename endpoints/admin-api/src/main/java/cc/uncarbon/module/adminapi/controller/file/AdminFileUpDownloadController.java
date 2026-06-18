package cc.uncarbon.module.adminapi.controller.file;

import cc.uncarbon.framework.helium.base.exception.BusinessException;
import cc.uncarbon.framework.helium.tenant.context.SimpleTenantContext;
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
import cc.uncarbon.module.tenant.model.valueobj.TenantMetaDTO;
import cc.uncarbon.module.tenant.service.TenantService;
import ch.qos.logback.core.util.FileSize;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.text.StrPool;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;


@Tag(name = "后台管理-文件上传、下载")
@RequestMapping(value = ApiPathPrefix.ADMIN)
@RequiredArgsConstructor
@RestController
@Slf4j
public class AdminFileUpDownloadController {

    /**
     * 上传路由，文本替换用
     */
    private static final String UPLOAD_ROUTE = "/v1/file/upload";

    /**
     * 如果下载传入该租户编码，视为忽略
     */
    private static final String DOWNLOAD_IGNORED_TENANT_CODE = StrPool.DASHED;

    /**
     * 下载路由，文本替换用
     * 增加 v1 前缀方便增加其他的传参约定
     */
    private static final String DOWNLOAD_ROUTE_HASHIDS = "/v1/file/download/{tenantCode}/{hashIds}";

    private final FileUpDownloadFacade fileUpDownloadFacade;
    private final HashidsHelper hashidsHelper;
    private final TenantService tenantService;

    /**
     * 从配置文件中获取的最大文件上传大小
     */
    @Value(value = "${spring.servlet.multipart.max-file-size:1024MB}")
    private String springServletMultipartMaxFileSize;


    @Operation(summary = "上传文件", tags = "")
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
            // 正常上传
            var options = new FacadeUploadOptions();
            options
                    .setOriginalFilename(file.getOriginalFilename())
                    .setContentType(file.getContentType())
                    .setDigestSha256(digest)
                    .setPlatform(null)
                    .setUseOriginalFilenameAsDownloadFileName(false);
            fileMeta = fileUpDownloadFacade.upload(file.getBytes(), options, attr);
        }

        return ApiResult.success(toUploadResult(
                fileMeta, request.getRequestURL().toString(), file.getOriginalFilename()));
    }

    @Operation(summary = "下载文件V1")
    @GetMapping(value = DOWNLOAD_ROUTE_HASHIDS)
    public void downloadV1(HttpServletResponse servletResponse,
                           @Parameter(description = "租户编码，用于多租户隔离；无租户时传" + DOWNLOAD_IGNORED_TENANT_CODE,
                                   example = DOWNLOAD_IGNORED_TENANT_CODE + ",91330105MACPN4X08Y")
                           @PathVariable String tenantCode,
                           @PathVariable String hashIds) throws IOException {
        Long id = hashidsHelper.decode(hashIds);
        FileErrorCodeEnum.A02006.throwIfNull(id);

        FileDownloadReply reply;
        if (DOWNLOAD_IGNORED_TENANT_CODE.equals(tenantCode)) {
            tenantCode = null;
        }
        reply = fileUpDownloadFacade.downloadById(tenantCode, id);

        if (reply.get().isRedirect2DirectUrl()) {
            // 302重定向
            servletResponse.sendRedirect(reply.get().getDirectUrl());
            return;
        }

        // 普通下载
        servletResponse.setHeader(Header.CONTENT_TYPE.getValue(), MediaType.APPLICATION_OCTET_STREAM_VALUE);
        servletResponse.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        String downFileName = URLEncoder.encode(reply.get().getStorageFilename(), StandardCharsets.UTF_8);
        servletResponse.setHeader(Header.CONTENT_DISPOSITION.getValue(), "attachment;filename=" + downFileName);
        // 把文件写入响应流
        IoUtil.write(servletResponse.getOutputStream(), false, reply.get().getFileBytes());
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
    private FileUploadResultVO toUploadResult(FileMetaDTO source, String requestUrl, String originalFilename) {
        FileUploadResultVO ret = new FileUploadResultVO()
                .setFileId(source.getId())
                .setFilename(source.getStorageFilenameFull())
                // 返回本次上传文件的原始文件名
                .setOriginalFilename(originalFilename);

        /*
        这里请根据实际业务性质调整
        有的业务出于安全目的，不能暴露直链，只能通过服务端代理下载后，返回 byte[]
        有的业务没有限制，上传后文件完全可以直接通过对象存储直链下载，如此还能节约服务端上传带宽
        有的业务有安全要求，只能通过预签名地址下载
        但本地存储又没有直链，只能通过文件ID；
        默认地，此处按【本地存储or对象存储直链为空：通过文件ID下载；对象存储：通过对象存储直链下载】返回 url
         */
        if (fileUpDownloadFacade.isLocalPlatform(source.getStorageCode())
                || CharSequenceUtil.isEmpty(source.getDirectUrl())
        ) {
            ret.setUrl(
                    // 默认接口风格为 RESTful，下载即为最后拼接“/{文件ID}”
                    String.format("%s/%s", requestUrl, source.getId())
            );
        } else {
            ret.setUrl(source.getDirectUrl());
        }
        return ret;
    }
}
