package cc.uncarbon.module.adminapi.props;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "admin-api.file-hashids")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class HashidsProperties {

    /**
     * 盐值
     */
    private String salt;

    /**
     * 输出最小长度
     */
    private Integer minLength = 8;

}
