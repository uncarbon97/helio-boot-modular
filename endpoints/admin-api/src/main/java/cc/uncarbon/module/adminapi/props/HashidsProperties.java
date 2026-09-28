package cc.uncarbon.module.adminapi.props;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "admin-api.file-hashids")
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class HashidsProperties {

    /**
     * 盐
     */
    private String salt;

    /**
     * 输出最小长度
     */
    private Integer minLength = 8;

}
