package cc.uncarbon.module.sys.service.impl;

import cc.uncarbon.framework.helium.base.exception.BusinessException;
import cc.uncarbon.framework.helium.base.page.PageResult;
import cc.uncarbon.module.commons.enums.DefaultErrorCodeEnum;
import cc.uncarbon.module.sys.dal.mapper.SysLoginLogMapper;
import cc.uncarbon.module.sys.entity.SysLogEntity;
import cc.uncarbon.module.sys.model.query.AdminSysLogListQuery;
import cc.uncarbon.module.sys.model.request.AdminInsertSysLogDTO;
import cc.uncarbon.module.sys.model.valueobj.SysLogBO;
import cc.uncarbon.module.sys.service.SysLogService;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;


/**
 * 系统日志
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class SysLogServiceImpl implements SysLogService {

    /**
     * UA可以接受的最大长度
     */
    public static final int USER_AGENT_MAX_LENGTH = 255;

    private final SysLoginLogMapper sysLoginLogMapper;


    /**
     * 后台管理-分页查询
     */
    @Override
    public PageResult<SysLogBO> adminList(AdminSysLogListQuery query) {
        Page<SysLogEntity> entityPage = sysLoginLogMapper.selectPage(
                new Page<>(query.getPageParam().getPageNum(), query.getPageParam().getPageSize()),
                new QueryWrapper<SysLogEntity>()
                        .lambda()
                        // 仅返回给前端少量字段
                        .select(SysLogEntity::getCreatedAt, SysLogEntity::getUsername, SysLogEntity::getOperation,
                                SysLogEntity::getIp, SysLogEntity::getStatus, SysLogEntity::getUserAgent,
                                SysLogEntity::getIpLocationRegionName, SysLogEntity::getIpLocationProvinceName,
                                SysLogEntity::getIpLocationCityName, SysLogEntity::getIpLocationDistrictName)
                        // 用户账号
                        .like(CharSequenceUtil.isNotBlank(query.getUsername()), SysLogEntity::getUsername, CharSequenceUtil.cleanBlank(query.getUsername()))
                        // 操作内容
                        .like(CharSequenceUtil.isNotBlank(query.getOperation()), SysLogEntity::getOperation, CharSequenceUtil.cleanBlank(query.getOperation()))
                        // 状态
                        .eq(ObjectUtil.isNotNull(query.getStatus()), SysLogEntity::getStatus, query.getStatus())
                        // 时间区间
                        .between(ObjectUtil.isNotNull(query.getBeginAt()) && ObjectUtil.isNotNull(query.getEndAt()), SysLogEntity::getCreatedAt, query.getBeginAt(), query.getEndAt())
                        // 排序
                        .orderByDesc(SysLogEntity::getId)
        );

        return this.entityPage2BOPage(entityPage);
    }

    /**
     * 根据 ID 取详情
     *
     * @param id 主键ID
     * @return null or 详情
     */
    @Override
    public SysLogBO getOneById(Long id) {
       return this.getOneById(id, false);
    }

    /**
     * 根据 ID 取详情
     *
     * @param id 主键ID
     * @param throwIfInvalidId 未找到时是否抛出异常
     * @return null or 详情
     */
    @Override
    public SysLogBO getOneById(Long id, boolean throwIfInvalidId) throws BusinessException {
        SysLogEntity entity = sysLoginLogMapper.selectById(id);
        if (throwIfInvalidId) {
            DefaultErrorCodeEnum.A00002.throwIfNull(entity);
        }

        return this.entity2BO(entity);
    }

    /**
     * 后台管理-新增
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Long adminCreate(AdminInsertSysLogDTO dto) {
        log.info("[后台管理-新增操作日志] >> 入参={}", dto);

        SysLogEntity entity = new SysLogEntity();
        BeanUtil.copyProperties(dto, entity);

        if (CharSequenceUtil.length(entity.getUserAgent()) > USER_AGENT_MAX_LENGTH) {
            // 超长度截断
            entity.setUserAgent(
                    CharSequenceUtil.subPre(entity.getUserAgent(), USER_AGENT_MAX_LENGTH)
            );
        }

        sysLoginLogMapper.insert(entity);

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
    private SysLogBO entity2BO(SysLogEntity entity) {
        if (entity == null) {
            return null;
        }

        SysLogBO bo = new SysLogBO();
        BeanUtil.copyProperties(entity, bo);

        // 按需改写字段

        return bo;
    }

    /**
     * 实体转值对象
     */
    private List<SysLogBO> entityList2BOs(List<SysLogEntity> entityList) {
        if (CollUtil.isEmpty(entityList)) {
            return List.of();
        }

        // 深拷贝
        List<SysLogBO> ret = new ArrayList<>(entityList.size());
        entityList.forEach(
                entity -> ret.add(this.entity2BO(entity))
        );

        return ret;
    }

    /**
     * 实体转值对象
     */
    private PageResult<SysLogBO> entityPage2BOPage(Page<SysLogEntity> entityPage) {
        return new PageResult<SysLogBO>()
                .setCurrent(entityPage.getCurrent())
                .setSize(entityPage.getSize())
                .setTotal(entityPage.getTotal())
                .setRecords(this.entityList2BOs(entityPage.getRecords()));
    }

}
