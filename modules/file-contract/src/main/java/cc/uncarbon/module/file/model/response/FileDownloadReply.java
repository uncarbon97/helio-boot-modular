package cc.uncarbon.module.file.model.response;

import cc.uncarbon.module.file.errorcode.FileErrorCodeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;


/**
 * 下载文件回复
 */
@Accessors(chain = true)
@RequiredArgsConstructor
@Data
public class FileDownloadReply implements Serializable {

    @Schema(description = "错误码")
    private final FileErrorCodeEnum errorCode;

    @Schema(description = "是否直接重定向到对象存储直链", title = "如果允许客户端直接从“对象存储直链”下载，则本字段可以置 true")
    private boolean redirect2DirectUrl;

    @Schema(description = "文件数据", title = "如果允许客户端直接从“对象存储直链”下载，则本字段可以置空")
    private byte[] fileBytes;

    @Schema(description = "对象存储直链")
    private String directUrl;

    @Schema(description = "存储文件名")
    private String storageFilename;

    public boolean isSuccess() {
        return errorCode == FileErrorCodeEnum.OK;
    }

}
