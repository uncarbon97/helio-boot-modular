package cc.uncarbon.module.sys.model.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * IP地址归属地
 */
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class IPLocationBO implements Serializable {

    @Schema(description = "IP地址归属地")
    private String location;

    /**
     * 未知属地
     */
    public static IPLocationBO unknown() {
        return new IPLocationBO("未知");
    }

    /**
     * 内网地址
     */
    public static IPLocationBO intranet() {
        return new IPLocationBO("内网");
    }

    /**
     * 位于中国内地
     */
    public static IPLocationBO inChina() {
        return new IPLocationBO("中国");
    }
}
