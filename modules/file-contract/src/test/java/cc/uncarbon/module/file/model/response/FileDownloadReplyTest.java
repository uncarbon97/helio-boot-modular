package cc.uncarbon.module.file.model.response;

import cc.uncarbon.module.file.errorcode.FileErrorCodeEnum;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * {@link FileDownloadReply} 单元测试
 */
class FileDownloadReplyTest {

    /**
     * 错误码为 OK 时应判定为成功
     */
    @Test
    void okErrorCodeMeansSuccess() {
        FileDownloadReply reply = new FileDownloadReply(FileErrorCodeEnum.OK);

        Assertions.assertTrue(reply.isSuccess());
        Assertions.assertSame(FileErrorCodeEnum.OK, reply.getErrorCode());
    }

    /**
     * 错误码非 OK 时应判定为失败
     */
    @Test
    void nonOkErrorCodeMeansFailure() {
        FileDownloadReply reply = new FileDownloadReply(FileErrorCodeEnum.A02006);

        Assertions.assertFalse(reply.isSuccess());
    }

    /**
     * 错误码为 null 时应判定为失败
     */
    @Test
    void nullErrorCodeMeansFailure() {
        Assertions.assertFalse(new FileDownloadReply(null).isSuccess());
    }

    /**
     * 链式 setter 应覆盖错误码以外的全部字段
     */
    @Test
    void chainSettersCoverRemainingFields() {
        byte[] bytes = {1, 2, 3};
        FileDownloadReply reply = new FileDownloadReply(FileErrorCodeEnum.OK)
                .setRedirect2DirectUrl(true)
                .setFileBytes(bytes)
                .setDirectUrl("https://example.com/x.png")
                .setStorageFilename("65f1c0a0d3b2.png");

        Assertions.assertTrue(reply.isRedirect2DirectUrl());
        Assertions.assertSame(bytes, reply.getFileBytes());
        Assertions.assertEquals("https://example.com/x.png", reply.getDirectUrl());
        Assertions.assertEquals("65f1c0a0d3b2.png", reply.getStorageFilename());
        Assertions.assertTrue(reply.isSuccess());
    }
}
