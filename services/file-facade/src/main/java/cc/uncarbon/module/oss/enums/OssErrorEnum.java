package cc.uncarbon.module.oss.enums;

import cc.uncarbon.framework.core.enums.BaseEnum;
import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;


/**
 * oss模块错误枚举
 */
@AllArgsConstructor
@Getter
public enum OssErrorEnum implements BaseEnum<Integer> {

    INVALID_ID(400, "无效ID"),
    FILE_UPLOAD_FAILED(500, "文件上传失败，请联系管理员"),
    FILE_DOWNLOAD_FAILED(500, "文件下载失败，请联系管理员"),;

    @EnumValue
    private final Integer value;
    private final String label;

}
