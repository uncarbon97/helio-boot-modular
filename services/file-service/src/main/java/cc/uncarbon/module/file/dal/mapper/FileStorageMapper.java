package cc.uncarbon.module.file.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import cc.uncarbon.module.file.dal.entity.FileStorageEntity;


/**
 * 文件存储点
 */
@Mapper
public interface FileStorageMapper extends BaseMapper<FileStorageEntity> {

}
