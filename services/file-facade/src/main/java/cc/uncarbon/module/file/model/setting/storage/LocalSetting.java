package cc.uncarbon.module.file.model.setting.storage;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * {@code org.dromara.x.file.storage.core.FileStorageProperties.LocalPlusConfig}
 */
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class LocalSetting implements FileStorageSetting {

    /**
     * 基础路径
     */
    private String basePath = "";

    /**
     * 存储路径，上传的文件都会存储在这个路径下面，默认"/"，注意"/"结尾
     */
    private String storagePath = "/";

    /**
     * 访问域名
     */
    private String domain = "";

}
