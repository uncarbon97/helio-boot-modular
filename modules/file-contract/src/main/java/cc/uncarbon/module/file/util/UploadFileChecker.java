package cc.uncarbon.module.file.util;

import cc.uncarbon.module.file.enums.UploadFileTypeEnum;
import cc.uncarbon.module.file.errorcode.FileErrorCodeEnum;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ArrayUtil;
import lombok.experimental.UtilityClass;
import org.jspecify.annotations.NonNull;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Locale;

/**
 * 前端上传文件检查器
 * 该检查器较为简易，请按照实际网络安全需要扩展
 */
@UtilityClass
public class UploadFileChecker {

    // 最小文件数量
    private static final int FILE_QTY_MIN = 1;
    // 最小文件尺寸
    private static final int FILE_SIZE_MIN = 1;
    // 原始文件名最大长度
    private static final int FILE_NAME_MAX_LENGTH = 255;
    // 截取的文件头字节数
    private static final int MAGIC_HEAD_BYTES = 16;

    // 文件尺寸 1MB
    public static final long FILE_SIZE_1MB = 1024 * 1024L;


    /**
     * 单个检查（按 {@link UploadFileTypeEnum} 白名单，含 magic-number 校验）
     *
     * @param singleFileSizeMax 最大单文件尺寸，单位=KB
     * @return {@link FileErrorCodeEnum#OK} 表示检查通过
     */
    public FileErrorCodeEnum check(@NonNull MultipartFile multipartFile, long singleFileSizeMax) {
        return checkSingle(multipartFile, singleFileSizeMax, null);
    }

    /**
     * 批量检查
     *
     * @param fileQtyMax        最大允许的文件数量
     * @param singleFileSizeMax 最大单文件尺寸，单位=KB
     * @param allFileSizeMax    最大多文件累计尺寸，单位=KB
     * @param allowedSuffixes   允许上传的文件后缀
     * @return {@link FileErrorCodeEnum#OK} 表示检查通过
     */
    public FileErrorCodeEnum check(Collection<MultipartFile> multipartFiles, int fileQtyMax,
                                   long singleFileSizeMax, long allFileSizeMax, String[] allowedSuffixes) {
        // 限制文件数量
        int fileQty = CollUtil.size(multipartFiles);
        if (fileQty < FILE_QTY_MIN) {
            return FileErrorCodeEnum.A02001;
        } else if (fileQty > fileQtyMax) {
            return FileErrorCodeEnum.A02002;
        }
        // 拦截 null 元素（须在累计大小求和前，避免方法引用触发 NPE）
        for (MultipartFile item : multipartFiles) {
            if (item == null) {
                return FileErrorCodeEnum.A02001;
            }
        }
        // 限制所有文件总大小（除以1024）
        long allFileSize = multipartFiles.stream().mapToLong(MultipartFile::getSize).sum() >> 10;
        if (allFileSize > allFileSizeMax) {
            return FileErrorCodeEnum.A02003;
        }

        // 每个文件单独检查
        for (MultipartFile item : multipartFiles) {
            FileErrorCodeEnum single = checkSingle(item, singleFileSizeMax, allowedSuffixes);
            if (single != FileErrorCodeEnum.OK) {
                return single;
            }
        }
        return FileErrorCodeEnum.OK;
    }

    /**
     * 单个检查
     *
     * @param singleFileSizeMax 最大单文件尺寸，单位=KB
     * @param allowedSuffixes   允许上传的文件后缀
     * @return {@link FileErrorCodeEnum#OK} 表示检查通过
     */
    public FileErrorCodeEnum check(@NonNull MultipartFile multipartFile, long singleFileSizeMax, String[] allowedSuffixes) {
        return checkSingle(multipartFile, singleFileSizeMax, allowedSuffixes);
    }

    /**
     * 统一单文件校验链
     *
     * @param allowedSuffixes 允许的后缀白名单；null=按 {@link UploadFileTypeEnum} 白名单
     */
    private FileErrorCodeEnum checkSingle(@NonNull MultipartFile multipartFile, long singleFileSizeMax,
                                          String[] allowedSuffixes) {
        FileErrorCodeEnum sizeCheck = checkSize(multipartFile.getSize(), singleFileSizeMax);
        if (sizeCheck != FileErrorCodeEnum.OK) {
            return sizeCheck;
        }

        FileErrorCodeEnum nameCheck = checkFileName(multipartFile.getOriginalFilename());
        if (nameCheck != FileErrorCodeEnum.OK) {
            return nameCheck;
        }

        String suffix = normalizedSuffix(multipartFile.getOriginalFilename());
        UploadFileTypeEnum fileType = UploadFileTypeEnum.ofExtend(suffix);
        // 检查后缀名
        boolean suffixAllowed = allowedSuffixes == null
                ? fileType != null
                : ArrayUtil.contains(allowedSuffixes, suffix);
        if (!suffixAllowed) {
            return FileErrorCodeEnum.A02004;
        }

        // 后缀能映射到已知类型时，校验文件头，拦截改后缀伪装的文件
        if (fileType != null) {
            FileErrorCodeEnum magicCheck = checkMagicNumber(multipartFile, fileType);
            if (magicCheck != FileErrorCodeEnum.OK) {
                return magicCheck;
            }
        }
        return FileErrorCodeEnum.OK;
    }

    /**
     * 确定文件大小区间
     */
    private FileErrorCodeEnum checkSize(long fileSize, long singleFileSizeMax) {
        if (fileSize < FILE_SIZE_MIN) {
            return FileErrorCodeEnum.A02005;
        }
        // 除以1024
        if ((fileSize >> 10) > singleFileSizeMax) {
            return FileErrorCodeEnum.A02003;
        }
        return FileErrorCodeEnum.OK;
    }

    /**
     * 原始文件名合法性：拦截路径穿越、空字节/控制字符（防响应头注入）、超长文件名
     */
    private FileErrorCodeEnum checkFileName(String originalFilename) {
        if (CharSequenceUtil.isBlank(originalFilename) || originalFilename.length() > FILE_NAME_MAX_LENGTH) {
            return FileErrorCodeEnum.A02011;
        }
        for (int i = 0; i < originalFilename.length(); i++) {
            char c = originalFilename.charAt(i);
            if (c < 0x20 || c == 0x7F || c == '/' || c == '\\') {
                return FileErrorCodeEnum.A02011;
            }
        }
        if (originalFilename.contains("..")) {
            return FileErrorCodeEnum.A02011;
        }
        return FileErrorCodeEnum.OK;
    }

    /**
     * 提取小写后缀名；无后缀返回空串
     */
    private String normalizedSuffix(String originalFilename) {
        String suffix = FileNameUtil.getSuffix(originalFilename);
        return CharSequenceUtil.isBlank(suffix) ? "" : suffix.toLowerCase(Locale.ROOT);
    }

    /**
     * 截取文件头前几个字节用于 magic-number 判断；读取失败按空头部处理（不匹配）
     */
    private FileErrorCodeEnum checkMagicNumber(@NonNull MultipartFile multipartFile, UploadFileTypeEnum fileType) {
        byte[] head = new byte[0];
        try (InputStream in = multipartFile.getInputStream()) {
            head = in.readNBytes(MAGIC_HEAD_BYTES);
        } catch (IOException e) {
            // 读不到文件头，按不匹配处理
        }
        return fileType.matchesMagicNumber(head) ? FileErrorCodeEnum.OK : FileErrorCodeEnum.A02004;
    }
}
