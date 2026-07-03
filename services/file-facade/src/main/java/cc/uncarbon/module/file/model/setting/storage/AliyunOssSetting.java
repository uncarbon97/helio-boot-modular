package cc.uncarbon.module.file.model.setting.storage;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * {@code org.dromara.x.file.storage.core.FileStorageProperties.AliyunOssConfig}
 */
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class AliyunOssSetting implements FileStorageSetting {

    private String accessKey;

    private String secretKey;

    private String endPoint;

    private String bucketName;

    /**
     * 访问域名
     */
    private String domain = "";

    /**
     * 基础路径
     */
    private String basePath = "";

    /**
     * 默认的 ACL
     */
    private String defaultAcl;

}
