package cc.uncarbon.module.file.mapper;

import cc.uncarbon.module.file.entity.FileStorageEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;


/**
 * 文件存储点
 */
@Mapper
public interface FileStorageMapper extends BaseMapper<FileStorageEntity> {

}
