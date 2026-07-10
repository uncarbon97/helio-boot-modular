package cc.uncarbon.module.file.enums;

import cc.uncarbon.framework.helium.base.enums.BaseEnum;
import cc.uncarbon.module.commons.enumdict.EnumDict;
import cc.uncarbon.module.file.model.setting.storage.*;
import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Locale;


/**
 * 存储平台类型枚举
 */
@EnumDict(name = "文件存储平台")
@AllArgsConstructor
@Getter
public enum PlatformTypeEnum implements BaseEnum<String> {

//    UNKNOWN("unknown", "未知"),
    LOCAL("LOCAL".toLowerCase(Locale.ROOT), "本地", LocalSetting.class),
    // FTP("FTP".toLowerCase(Locale.ROOT), "FTP"),
    // SFTP("SFTP".toLowerCase(Locale.ROOT), "SFTP"),
    // WEBDAV("WEBDAV".toLowerCase(Locale.ROOT), "WebDAV"),
    AMAZON_S3("s3", "S3 兼容", AmazonS3Setting.class),
    AMAZON_S3_V2("s3-v2", "S3-v2 兼容", AmazonS3V2Setting.class),
//    MinIO("MinIO".toLowerCase(Locale.ROOT), "MinIO"),
    ALIYUN_OSS("ALIYUN_OSS".toLowerCase(Locale.ROOT), "阿里云 OSS", AliyunOssSetting.class),
//    HUAWEI_OBS("HUAWEI_OBS".toLowerCase(Locale.ROOT), "华为云 OBS"),
//    TENCENT_COS("TENCENT_COS".toLowerCase(Locale.ROOT), "腾讯云 COS"),
//    BAIDU_BOS("BAIDU_BOS".toLowerCase(Locale.ROOT), "百度云 BOS"),
//    UPYUN_USS("UPYUN_USS".toLowerCase(Locale.ROOT), "又拍云 USS"),
//    QINIU_KODO("QINIU_KODO".toLowerCase(Locale.ROOT), "七牛云 Kodo"),
//    GOOGLE_CLOUD_STORAGE("GOOGLE_CLOUD_STORAGE".toLowerCase(Locale.ROOT), "GoogleCloud Storage"),
//    FAST_DFS("FAST_DFS".toLowerCase(Locale.ROOT), "FastDFS"),
//    AZURE_BLOB_STORAGE("AZURE_BLOB_STORAGE".toLowerCase(Locale.ROOT), "Azure Blob Storage"),
//    MONGO_GRID_FS("MONGO_GRID_FS".toLowerCase(Locale.ROOT), "Mongo GridFS"),
//    GO_FASTDFS("GO_FASTDFS".toLowerCase(Locale.ROOT), "go-fastdfs"),
//    VOLCENGINE_TOS("VOLCENGINE_TOS".toLowerCase(Locale.ROOT), "火山引擎 TOS"),

    ;@EnumValue
    private final String value;
    private final String label;

    /**
     * 相关设置类
     */
    private final Class<? extends FileStorageSetting> settingClass;

}
