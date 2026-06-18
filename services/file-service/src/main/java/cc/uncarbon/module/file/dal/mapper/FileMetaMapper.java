package cc.uncarbon.module.file.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import cc.uncarbon.module.file.dal.entity.FileMetaEntity;


/**
 * 文件元数据
 */
@Mapper
public interface FileMetaMapper extends BaseMapper<FileMetaEntity> {

}
