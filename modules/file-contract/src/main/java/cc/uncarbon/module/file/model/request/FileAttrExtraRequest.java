package cc.uncarbon.module.file.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;


/**
 * 指定文件附加属性
 * 只在上传文件时有效
 */
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class FileAttrExtraRequest implements Serializable {

    @Schema(description = "文件主分类", example = "id_card=身份证 driver_license=驾驶证")
    private String category;

}
