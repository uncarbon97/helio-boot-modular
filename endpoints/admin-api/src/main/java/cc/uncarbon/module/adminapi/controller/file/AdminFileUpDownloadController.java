package cc.uncarbon.module.adminapi.controller.file;

import cc.uncarbon.framework.helium.base.exception.BusinessException;
import cc.uncarbon.framework.helium.web.model.response.ApiResult;
import cc.uncarbon.module.adminapi.helper.FileUploadResultHelper;
import cc.uncarbon.module.adminapi.model.valueobj.FileUploadResultVO;
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
import cn.hutool.core.io.file.FileNameUtil;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.servlet.autoconfigure.MultipartProperties;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;


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
     * 扩展名白名单对应的 Content-Type 映射；未知扩展一律按二进制流处理
     */
    private static final Map<String, String> EXT_CONTENT_TYPE_MAP = Map.of(
            "jpg", "image/jpeg",
            "png", "image/png",
            "webp", "image/webp",
            "gif", "image/gif",
            "xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    );

    private final FileUpDownloadFacade fileUpDownloadFacade;
    private final FileUploadResultHelper fileUploadResultHelper;
    private final MultipartProperties multipartProps;


    @Operation(summary = "上传文件")
    @PostMapping(value = UPLOAD_ROUTE)
    // 约束：登录后才能上传
    @SaCheckLogin(type = StpLoginType.ADMIN)
    public ApiResult<FileUploadResultVO> upload(
            @RequestPart MultipartFile file, @RequestPart(required = false) @Valid FileAttrExtraRequest attr,
            HttpServletRequest request
    ) throws IOException {
        checkBeforeUpload(file);

        // 流式计算摘要；multipart 已由容器落盘，getInputStream() 每次返回新流，整文件不驻留应用堆内存
        String digest = DigestUtil.sha256Hex(file.getInputStream());
        FileMetaDTO fileMeta = fileUpDownloadFacade.findByDigestSha256(digest);
        if (fileMeta == null) {
            // 不存在，走正常上传：再次从 multipart 取流，由底层存储平台流式消费
            var options = new FacadeUploadOptions();
            options
                    .setOriginalFilename(file.getOriginalFilename())
                    // 不信任客户端 Content-Type，按扩展名白名单推导，防止直链存储点存储型 XSS
                    .setContentType(resolveContentType(file.getOriginalFilename()))
                    .setDigestSha256(digest)
                    .setPlatform(null)
                    .setUseOriginalFilenameAsDownloadFileName(false);
            fileMeta = fileUpDownloadFacade.upload(file.getInputStream(), file.getSize(), options, attr);
        }
        return ApiResult.success(fileUploadResultHelper.toUploadResult(fileMeta, request));
    }

    @Operation(summary = "下载文件V1")
    @GetMapping(value = FileUploadResultHelper.DOWNLOAD_ROUTE_HASHIDS)
    public void downloadV1(HttpServletResponse servletResponse,
                           @PathVariable String hashIds,
                           @Parameter(description = "租户编码，用于多租户隔离；无租户时不传",
                                   example = "91330105MACPN4X08Y")
                           @PathVariable(required = false) String tenantCode) throws IOException {
        Long id = fileUploadResultHelper.getHashidsHelper().decode(hashIds);
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

    /**
     * 按原始文件名扩展名推导 Content-Type，不信任客户端上报值
     */
    private String resolveContentType(String originalFilename) {
        String ext = FileNameUtil.getSuffix(originalFilename);
        if (ext != null) {
            ext = ext.toLowerCase(Locale.ROOT);
        }
        return EXT_CONTENT_TYPE_MAP.getOrDefault(ext, MediaType.APPLICATION_OCTET_STREAM_VALUE);
    }

    private void checkBeforeUpload(MultipartFile file) {
        FileErrorCodeEnum.A02001.throwIfNull(file);
        long kbMax = multipartProps.getMaxFileSize().toKilobytes();
        var errorCodeEnum = UploadFileChecker.check(file,
                // 默认只能上传尺寸之内的文件
                kbMax,
                // 且约束后缀名
                UploadFileChecker.COMMON_EXTEND_NAMES);
        if (errorCodeEnum != FileErrorCodeEnum.OK) {
            throw new BusinessException(errorCodeEnum);
        }
    }
}
