package cc.uncarbon.module.file.util;

import cc.uncarbon.module.file.errorcode.FileErrorCodeEnum;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

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
        List<MultipartFile> files = List.of(file("a.jpg", 100, JPEG_MAGIC), file("b.png", 200, PNG_MAGIC));
        Assertions.assertEquals(FileErrorCodeEnum.OK,
                UploadFileChecker.check(files, 3, 1024, 2048, new String[]{"jpg", "png"}));
    }

    @Test
    void batchShortCircuitsOnFirstBadFile() {
        List<MultipartFile> files = List.of(file("ok.jpg", 100, JPEG_MAGIC), file("bad.exe", 100));
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
                UploadFileChecker.check(file("a.JPG", 100, JPEG_MAGIC), 1024, new String[]{"jpg"}));
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

    @Test
    void twoArgOverloadUsesEnumWhitelist() {
        // 已知类型后缀 → 走 magic-number 校验
        Assertions.assertEquals(FileErrorCodeEnum.OK,
                UploadFileChecker.check(file("a.jpg", 100, JPEG_MAGIC), 1024));
        // 未知类型后缀不在枚举白名单内
        Assertions.assertEquals(FileErrorCodeEnum.A02004,
                UploadFileChecker.check(file("a.doc", 100), 1024));
    }

    @Test
    void customSuffixUnknownTypeSkipsMagicCheck() {
        // 自定义白名单含未知类型后缀 → 无 magic-number 可比对，直接放行
        Assertions.assertEquals(FileErrorCodeEnum.OK,
                UploadFileChecker.check(file("a.doc", 100, new byte[]{1, 2, 3}), 1024, new String[]{"doc"}));
    }

    @Test
    void magicNumberMismatchRejected() {
        // 改后缀伪装：jpg 后缀但内容是 PNG 文件头
        Assertions.assertEquals(FileErrorCodeEnum.A02004,
                UploadFileChecker.check(file("fake.jpg", 100, PNG_MAGIC), 1024, new String[]{"jpg"}));
    }

    @Test
    void unreadableStreamTreatedAsMagicMismatch() {
        Assertions.assertEquals(FileErrorCodeEnum.A02004,
                UploadFileChecker.check(unreadableFile("a.jpg", 100), 1024, new String[]{"jpg"}));
    }

    @Test
    void blankFilenameRejected() {
        Assertions.assertEquals(FileErrorCodeEnum.A02011,
                UploadFileChecker.check(file(" ", 100), 1024, new String[]{"jpg"}));
    }

    @Test
    void overlongFilenameRejected() {
        Assertions.assertEquals(FileErrorCodeEnum.A02011,
                UploadFileChecker.check(file("a".repeat(256) + ".jpg", 100), 1024, new String[]{"jpg"}));
    }

    @Test
    void controlCharFilenameRejected() {
        // 逐类拦截：c<0x20、c==0x7F、'/'、'\'
        for (String bad : new String[]{"a\nb.jpg", "a\u007F.jpg", "a/b.jpg", "a\\b.jpg"}) {
            Assertions.assertEquals(FileErrorCodeEnum.A02011,
                    UploadFileChecker.check(file(bad, 100), 1024, new String[]{"jpg"}), "文件名: " + bad);
        }
    }

    @Test
    void pathTraversalFilenameRejected() {
        Assertions.assertEquals(FileErrorCodeEnum.A02011,
                UploadFileChecker.check(file("a..b.jpg", 100), 1024, new String[]{"jpg"}));
    }

    @Test
    void batchNullItemRejected() {
        List<MultipartFile> files = Arrays.asList(file("a.jpg", 100, JPEG_MAGIC), null);
        Assertions.assertEquals(FileErrorCodeEnum.A02001,
                UploadFileChecker.check(files, 3, 1024, 2048, new String[]{"jpg"}));
    }


    /*
    ----------------------------------------------------------------
                        私有方法
    ----------------------------------------------------------------
     */

    private static final byte[] JPEG_MAGIC = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG_MAGIC = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

    private MultipartFile file(String filename, long size) {
        return file(filename, size, new byte[0]);
    }

    private MultipartFile file(String filename, long size, byte[] content) {
        return new MockMultipartFile("file", filename, null, content) {
            @Override
            public long getSize() {
                return size;
            }
        };
    }

    /**
     * 文件头流读取即失败的文件（覆盖 IOException 按不匹配处理分支）
     */
    private MultipartFile unreadableFile(String filename, long size) {
        return new MockMultipartFile("file", filename, null, new byte[0]) {
            @Override
            public long getSize() {
                return size;
            }

            @Override
            public InputStream getInputStream() throws IOException {
                throw new IOException("unreadable");
            }
        };
    }
}
