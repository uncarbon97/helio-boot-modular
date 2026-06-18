package cc.uncarbon.module.file.service;

import cc.uncarbon.module.commons.constant.SQLSegment;
import cc.uncarbon.module.file.constant.OssConstant;
import cc.uncarbon.module.file.dal.entity.FileMetaEntity;
import cc.uncarbon.module.file.dal.mapper.FileMetaMapper;
import cc.uncarbon.module.file.model.request.FileAttrExtraRequest;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.CharSequenceUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


/**
 * 上传文件信息
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OssFileInfoService {

    private final FileMetaMapper fileMetaMapper;
    private final FileStorageService fileStorageService;
    
    /**
     * 根据 MD5 取文件信息
     */
    public FileMetaDTO getOneByMd5(String md5) {
        FileMetaEntity entity = fileMetaMapper.selectOne(new LambdaQueryWrapper<FileMetaEntity>()
                .eq(FileMetaEntity::getMd5, md5)
                .last(SQLSegment.LIMIT_1)
        );

        return this.entity2BO(entity);
    }

    /**
     * 上传成功后，保存文件记录
     *
     * @return 文件ID
     */
    @Transactional(rollbackFor = Exception.class)
    public FileMetaDTO save(@NonNull FileInfo fileInfo, @NonNull FileAttrExtraRequest attr) {
        FileMetaEntity entity = new FileMetaEntity()
                .setStoragePlatform(fileInfo.getPlatform())
                .setStorageBasePath(fileInfo.getBasePath())
                .setStoragePath(fileInfo.getPath())
                // 需要裁切掉后缀名
                .setStorageFilename(FileUtil.getPrefix(fileInfo.getFilename()))
                .setOriginalFilename(FileUtil.getPrefix(fileInfo.getOriginalFilename()))
                .setExtendName(fileInfo.getExt())
                .setFileSize(fileInfo.getSize())
                .setMd5(attr.getMd5())
                .setClassified(attr.getClassified());

        if (!isLocalPlatform(fileInfo.getPlatform())) {
            // 非本地存储，保存对象存储直链
            entity.setDirectUrl(fileInfo.getUrl());
        }
        fileMetaMapper.insert(entity);
        return this.entity2BO(entity);
    }

    /**
     * 转换为 FileInfo 对象
     */
    public static FileInfo toFileInfo(FileMetaDTO source) {
        FileInfo fileInfo = new FileInfo();
        fileInfo.setPlatform(source.getStoragePlatform());
        fileInfo.setBasePath(source.getStorageBasePath());
        fileInfo.setPath(source.getStoragePath());
        fileInfo.setFilename(source.getStorageFilenameFull());
        fileInfo.setSize(source.getFileSize());
        fileInfo.setOriginalFilename(source.getOriginalFilenameFull());
        return fileInfo;
    }

    /**
     * 转换为 FileInfo 对象
     */
    public static FileInfo toFileInfo(FileMetaEntity source) {
        FileInfo fileInfo = new FileInfo();
        fileInfo.setPlatform(source.getStoragePlatform());
        fileInfo.setBasePath(source.getStorageBasePath());
        fileInfo.setPath(source.getStoragePath());
        fileInfo.setFilename(source.getStorageFilenameFull());
        fileInfo.setSize(source.getFileSize());
        fileInfo.setOriginalFilename(source.getOriginalFilenameFull());
        return fileInfo;
    }

    /**
     * 是否为本地存储平台
     *
     * @param storagePlatform 存储平台名
     */
    public static boolean isLocalPlatform(String storagePlatform) {
        return CharSequenceUtil.startWith(storagePlatform, OssConstant.PLATFORM_PREFIX_LOCAL);
    }
}
