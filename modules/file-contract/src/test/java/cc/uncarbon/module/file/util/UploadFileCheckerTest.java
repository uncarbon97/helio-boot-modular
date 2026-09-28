package cc.uncarbon.module.file.util;

import cc.uncarbon.module.file.errorcode.FileErrorCodeEnum;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import org.mockito.Mockito;

/**
 * {@link UploadFileChecker} 上传文件校验分支测试
 */
@ExtendWith(MockitoExtension.class)
class UploadFileCheckerTest {

    @Mock
    private MultipartFile mockedEmptySizeFile;


    @Test
    void batchEmptyCollectionRejected() {
        Assertions.assertEquals(FileErrorCodeEnum.A02001,
                UploadFileChecker.check(List.of(), 3, 1024, 2048, new String[]{"jpg"}));
    }

    @Test
    void batchTooManyFilesRejected() {
        List<MultipartFile> files = List.of(file("a.jpg", 100), file("b.jpg", 100), file("c.jpg", 100));
        Assertions.assertEquals(FileErrorCodeEnum.A02002,
                UploadFileChecker.check(files, 2, 1024, 2048, new String[]{"jpg"}));
    }

    @Test
    void batchTotalSizeOverflowRejected() {
        List<MultipartFile> files = List.of(file("a.jpg", 1024 * 600), file("b.jpg", 1024 * 600));
        Assertions.assertEquals(FileErrorCodeEnum.A02003,
                UploadFileChecker.check(files, 3, 1024, 1024, new String[]{"jpg"}));
    }

    @Test
    void batchAllPassReturnsOK() {
        List<MultipartFile> files = List.of(file("a.jpg", 100), file("b.png", 200));
        Assertions.assertEquals(FileErrorCodeEnum.OK,
                UploadFileChecker.check(files, 3, 1024, 2048, new String[]{"jpg", "png"}));
    }

    @Test
    void batchShortCircuitsOnFirstBadFile() {
        List<MultipartFile> files = List.of(file("ok.jpg", 100), file("bad.exe", 100));
        Assertions.assertEquals(FileErrorCodeEnum.A02004,
                UploadFileChecker.check(files, 3, 1024, 2048, new String[]{"jpg"}));
    }

    @Test
    void singleEmptyFileRejected() {
        Mockito.when(mockedEmptySizeFile.getSize()).thenReturn(0L);
        Assertions.assertEquals(FileErrorCodeEnum.A02005,
                UploadFileChecker.check(mockedEmptySizeFile, 1024, new String[]{"jpg"}));
    }

    @Test
    void singleOversizeRejected() {
        Assertions.assertEquals(FileErrorCodeEnum.A02003,
                UploadFileChecker.check(file("a.jpg", 1024L * 2048), 1024, new String[]{"jpg"}));
    }

    @Test
    void singleSuffixCaseInsensitive() {
        Assertions.assertEquals(FileErrorCodeEnum.OK,
                UploadFileChecker.check(file("a.JPG", 100), 1024, new String[]{"jpg"}));
    }

    @Test
    void singleSuffixNotAllowedRejected() {
        Assertions.assertEquals(FileErrorCodeEnum.A02004,
                UploadFileChecker.check(file("a.exe", 100), 1024, new String[]{"jpg", "png"}));
    }

    @Test
    void singleNoSuffixRejected() {
        Assertions.assertEquals(FileErrorCodeEnum.A02004,
                UploadFileChecker.check(file("filename", 100), 1024, new String[]{"jpg"}));
    }


    /*
    ----------------------------------------------------------------
                        私有方法 private methods
    ----------------------------------------------------------------
     */

    private MultipartFile file(String filename, long size) {
        return new MockMultipartFile("file", filename, null, new byte[0]) {
            @Override
            public long getSize() {
                return size;
            }
        };
    }
}
