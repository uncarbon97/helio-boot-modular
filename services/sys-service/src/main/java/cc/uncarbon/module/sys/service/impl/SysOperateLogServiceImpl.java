package cc.uncarbon.module.sys.service.impl;

import cc.uncarbon.framework.helium.base.page.PageResult;
import cc.uncarbon.module.commons.exception.NoRecordException;
import cc.uncarbon.module.commons.resoler.IPLocationResolver;
import cc.uncarbon.module.sys.dal.entity.SysOperateLogEntity;
import cc.uncarbon.module.sys.dal.mapper.SysOperateLogMapper;
import cc.uncarbon.module.sys.model.query.AdminSysOperateLogListQuery;
import cc.uncarbon.module.sys.model.request.SysOperateLogCreateRequest;
import cc.uncarbon.module.sys.model.valueobj.SysOperateLogDTO;
import cc.uncarbon.module.sys.service.SysOperateLogService;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;


/**
 * 系统操作日志
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class SysOperateLogServiceImpl implements SysOperateLogService {

    /**
     * UA可以接受的最大长度
     */
    public static final int USER_AGENT_MAX_LENGTH = 255;

    private final SysOperateLogMapper sysOperateLogMapper;
    private final IPLocationResolver ipLocationResolver;


    @Override
    public PageResult<SysOperateLogDTO> adminList(AdminSysOperateLogListQuery query) {
        Page<SysOperateLogEntity> entityPage = sysOperateLogMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()),
                new LambdaQueryWrapper<SysOperateLogEntity>()
                        // 业务类型
                        .like(CharSequenceUtil.isNotBlank(query.getBizType()), SysOperateLogEntity::getBizType, CharSequenceUtil.cleanBlank(query.getBizType()))
                        // 行为
                        .like(CharSequenceUtil.isNotBlank(query.getBehavior()), SysOperateLogEntity::getBehavior, CharSequenceUtil.cleanBlank(query.getBehavior()))
                        // 业务号
                        .like(CharSequenceUtil.isNotBlank(query.getBizNo()), SysOperateLogEntity::getBizNo, CharSequenceUtil.cleanBlank(query.getBizNo()))
                        // 用户ID
                        .eq(Objects.nonNull(query.getUserId()), SysOperateLogEntity::getUserId, query.getUserId())
                        // 结果状态
                        .eq(Objects.nonNull(query.getResultStatus()), SysOperateLogEntity::getResultStatus, query.getResultStatus())
                        // 时间区间
                        .between(Objects.nonNull(query.getBeginAt()) && Objects.nonNull(query.getEndAt()), SysOperateLogEntity::getCreatedAt, query.getBeginAt(), query.getEndAt())
                        // 排序
                        .orderByDesc(SysOperateLogEntity::getId)
        );
        return convertPage(entityPage);
    }

    @Override
    public SysOperateLogDTO getById(Long id) {
        if (id == null) return null;
        var entity = sysOperateLogMapper.selectById(id);
        return convertEntity(entity);
    }

    @Override
    public SysOperateLogDTO getNonnullById(Long id) throws NoRecordException {
        return NoRecordException.throwIfNull(getById(id));
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Long create(SysOperateLogCreateRequest request) {
        var entity = new SysOperateLogEntity();
        BeanUtil.copyProperties(request, entity);
        // 按需改写字段
        String ua = entity.getVisitorUserAgent();
        if (CharSequenceUtil.length(ua) > USER_AGENT_MAX_LENGTH) {
            // 超长度截断
            ua = CharSequenceUtil.subPre(ua, USER_AGENT_MAX_LENGTH);
        }

        String ipLocation = null;
        if (Objects.nonNull(entity.getVisitorIp())) {
            ipLocation = ipLocationResolver.resolve(entity.getVisitorIp());
        }

        entity.setVisitorUserAgent(ua)
                .setVisitorIpLocation(ipLocation);

        sysOperateLogMapper.insert(entity);
        return entity.getId();
    }


    /*
    ----------------------------------------------------------------
                        私有方法 private methods
    ----------------------------------------------------------------
     */

    /**
     * 实体转值对象
     */
    private SysOperateLogDTO convertEntity(SysOperateLogEntity entity) {
        if (entity == null) return null;

        var ret = new SysOperateLogDTO();
        BeanUtil.copyProperties(entity, ret);
        // 按需改写字段
        return ret;
    }

    /**
     * 实体转值对象
     */
    private List<SysOperateLogDTO> convertList(List<SysOperateLogEntity> entityList) {
        if (CollUtil.isEmpty(entityList)) {
            return List.of();
        }
        return entityList.stream().map(this::convertEntity).toList();
    }

    /**
     * 实体转值对象
     */
    private PageResult<SysOperateLogDTO> convertPage(Page<SysOperateLogEntity> entityPage) {
        return new PageResult<SysOperateLogDTO>()
                .setCurrent(entityPage.getCurrent())
                .setSize(entityPage.getSize())
                .setTotal(entityPage.getTotal())
                .setRecords(convertList(entityPage.getRecords()));
    }

}
