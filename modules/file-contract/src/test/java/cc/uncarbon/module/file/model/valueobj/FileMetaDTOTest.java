package cc.uncarbon.module.file.model.valueobj;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Instant;

/**
 * {@link FileMetaDTO} 单元测试
 */
class FileMetaDTOTest {

    /**
     * 有扩展名时 getStorageFilenameFull 应拼接扩展名
     */
    @Test
    void storageFilenameFullAppendsExtendName() {
        FileMetaDTO dto = new FileMetaDTO()
                .setStorageFilename("65f1c0a0d3b2")
                .setExtendName("png");

        Assertions.assertEquals("65f1c0a0d3b2.png", dto.getStorageFilenameFull());
    }

    /**
     * 有扩展名时 getOriginalFilenameFull 应拼接扩展名
     */
    @Test
    void originalFilenameFullAppendsExtendName() {
        FileMetaDTO dto = new FileMetaDTO()
                .setOriginalFilename("头像")
                .setExtendName("jpg");

        Assertions.assertEquals("头像.jpg", dto.getOriginalFilenameFull());
    }

    /**
     * 扩展名为 null 时应原样返回不带扩展名的文件名
     */
    @Test
    void nullExtendNameReturnsBareFilename() {
        FileMetaDTO dto = new FileMetaDTO()
                .setStorageFilename("noext")
                .setOriginalFilename("文档");

        Assertions.assertNull(dto.getExtendName());
        Assertions.assertEquals("noext", dto.getStorageFilenameFull());
        Assertions.assertEquals("文档", dto.getOriginalFilenameFull());
    }

    /**
     * 扩展名为空串时应原样返回不带扩展名的文件名
     */
    @Test
    void emptyExtendNameReturnsBareFilename() {
        FileMetaDTO dto = new FileMetaDTO()
                .setStorageFilename("noext")
                .setExtendName("");

        Assertions.assertEquals("noext", dto.getStorageFilenameFull());
    }

    /**
     * 全参构造器应覆盖全部 16 个字段
     */
    @Test
    void allArgsConstructorCoversAllFields() {
        Instant now = Instant.now();
        FileMetaDTO dto = new FileMetaDTO(1L, now, "小明", now, "小红", 10L, "main", "/data", "sub/",
                "65f1c0a0d3b2", "头像", "png", 1024L,
                "digest", "avatar", "https://example.com/x.png");

        Assertions.assertEquals(1L, dto.getId());
        Assertions.assertEquals(now, dto.getCreatedAt());
        Assertions.assertEquals(now, dto.getUpdatedAt());
        Assertions.assertEquals(10L, dto.getStorageId());
        Assertions.assertEquals("main", dto.getStorageCode());
        Assertions.assertEquals("/data", dto.getStorageBasePath());
        Assertions.assertEquals("sub/", dto.getSubDirPath());
        Assertions.assertEquals("65f1c0a0d3b2", dto.getStorageFilename());
        Assertions.assertEquals("头像", dto.getOriginalFilename());
        Assertions.assertEquals("png", dto.getExtendName());
        Assertions.assertEquals(1024L, dto.getFileSize());
        Assertions.assertEquals("digest", dto.getDigestSha256());
        Assertions.assertEquals("avatar", dto.getCategory());
        Assertions.assertEquals("https://example.com/x.png", dto.getDirectUrl());
        Assertions.assertEquals("小明", dto.getCreatedBy());
        Assertions.assertEquals("小红", dto.getUpdatedBy());
    }

    /**
     * Lombok equals/hashCode 应按全字段比较
     */
    @Test
    void lombokEqualsAndHashCode() {
        FileMetaDTO left = new FileMetaDTO().setStorageFilename("a").setExtendName("txt");
        FileMetaDTO right = new FileMetaDTO().setStorageFilename("a").setExtendName("txt");

        Assertions.assertEquals(left, right);
        Assertions.assertEquals(left.hashCode(), right.hashCode());
        Assertions.assertNotEquals(left, new FileMetaDTO().setStorageFilename("b"));
    }
}
