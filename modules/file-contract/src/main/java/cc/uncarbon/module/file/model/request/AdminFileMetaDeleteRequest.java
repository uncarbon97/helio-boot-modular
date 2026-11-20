package cc.uncarbon.module.file.model.request;

import cc.uncarbon.framework.helium.db.enums.YesOrNoEnum;
import cc.uncarbon.module.commons.model.request.IdRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Objects;


/**
 * 删除文件元数据入参
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class AdminFileMetaDeleteRequest extends IdRequest<Long> implements Serializable {

    @Schema(description = "是否一并删除远端原始文件；远端文件不存在或已被删除时，仍直接删除元数据")
    private YesOrNoEnum deleteRemoteFile;
    public YesOrNoEnum getDeleteRemoteFile() {
        return Objects.requireNonNullElse(deleteRemoteFile, YesOrNoEnum.NO);
    }
}
