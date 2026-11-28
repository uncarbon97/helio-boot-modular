package cc.uncarbon.module.sys.service.impl;

import cc.uncarbon.framework.helium.base.page.PageResult;
import cc.uncarbon.framework.helium.db.mybatisplus.conditions.query.LambdaQueryWrapperX;
import cc.uncarbon.framework.helium.excel.exporter.ExcelBatchExporter;
import cc.uncarbon.framework.helium.excel.page.CursorPageParam;
import cc.uncarbon.module.commons.errorcode.DefaultErrorCodeEnum;
import cc.uncarbon.module.commons.exception.NoRecordException;
import cc.uncarbon.module.commons.facade.AuditNicknameFiller;
import cc.uncarbon.module.sys.dal.entity.SysOperateLogEntity;
import cc.uncarbon.module.sys.dal.mapper.SysOperateLogMapper;
import cc.uncarbon.module.sys.iplocation.IPLocationResolver;
import cc.uncarbon.module.sys.model.excel.SysOperateLogExcelRow;
import cc.uncarbon.module.sys.model.query.AdminSysOperateLogListQuery;
import cc.uncarbon.module.sys.model.request.AdminSysOperateLogExportRequest;
import cc.uncarbon.module.sys.model.request.SysOperateLogCreateRequest;
import cc.uncarbon.module.sys.model.valueobj.SysOperateLogDTO;
import cc.uncarbon.module.sys.service.SysOperateLogService;
import cc.uncarbon.module.sys.service.SysUserService;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.OutputStream;
import java.util.List;
import java.util.Objects;
import java.util.Set;


/**
 * 系统操作日志
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class SysOperateLogServiceImpl implements SysOperateLogService {

    /**
     * UA长度上限
     */
    public static final int USER_AGENT_MAX_LENGTH = 250;

    private final SysOperateLogMapper sysOperateLogMapper;
    private final SysUserService sysUserService;
    private final List<IPLocationResolver> ipLocationResolvers;
    private final AuditNicknameFiller auditNicknameFiller;


    @Override
    public PageResult<SysOperateLogDTO> adminList(AdminSysOperateLogListQuery query) {
        // 过滤查询条件中的用户ID，防止越权枚举隐藏用户
        sysUserService.checkUserQueryAccess(query.getUserIds());

        // 兼容游标导出
        Long lastId = query.getPageParam() instanceof CursorPageParam cursorParam ? cursorParam.getLastId() : null;

        Page<SysOperateLogEntity> entityPage = sysOperateLogMapper.selectPage(
                new Page<SysOperateLogEntity>(query.getPageNum(), query.getPageSize()).setSearchCount(lastId == null),
                new LambdaQueryWrapperX<SysOperateLogEntity>()
                        // 兼容游标导出
                        .ltIfPresent(SysOperateLogEntity::getId, lastId)
                        // 业务类型
                        .likeIfPresent(SysOperateLogEntity::getBizType, query.getBizType())
                        // 行为
                        .likeIfPresent(SysOperateLogEntity::getBehavior, query.getBehavior())
                        // 业务号
                        .likeIfPresent(SysOperateLogEntity::getBizNo, query.getBizNo())
                        // 操作人
                        .inIfPresent(SysOperateLogEntity::getUserId, query.getUserIds())
                        // 结果状态
                        .eqIfPresent(SysOperateLogEntity::getResultStatus, query.getResultStatus())
                        // 时间区间
                        .betweenIfPresent(SysOperateLogEntity::getCreatedAt, query.getBeginAt(), query.getEndAt())
                        // 排序
                        .orderByDesc(SysOperateLogEntity::getId)
        );
        return convertPage(entityPage);
    }

    @Override
    public SysOperateLogDTO getById(Long id) {
        if (id == null) return null;
        var entity = sysOperateLogMapper.selectById(id);
        return auditNicknameFiller.fillByUserIdOne(entity, this::convertEntity);
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
            for (IPLocationResolver locationResolver : ipLocationResolvers) {
                ipLocation = locationResolver.resolve(entity.getVisitorIp());
                if (ipLocation != null) break;
            }
        }

        entity.setVisitorUserAgent(ua)
                .setVisitorIpLocation(ipLocation);

        sysOperateLogMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public void adminExportExcel(AdminSysOperateLogExportRequest request, OutputStream out) {
        // 单次导出行数上限
        final int exportMaxRows = 100_000;
        // 导出单批行数
        final int exportBatchSize = 1000;

        boolean includeUserAgent = Boolean.TRUE.equals(request.getIncludeUserAgent());

        ExcelBatchExporter.exportByRow(out, "Sheet1", SysOperateLogExcelRow.class,
                includeUserAgent ? null : Set.of("visitorUserAgent"),
                exportMaxRows, DefaultErrorCodeEnum.A00008,
                cursor -> {
                    request.setPageParam(new CursorPageParam(1, exportBatchSize, cursor));
                    var pageResult = adminList(request);
                    var records = pageResult.getRecords();
                    return ExcelBatchExporter.PageBatch.of(
                            records.stream().map(dto -> SysOperateLogExcelRow.of(dto, includeUserAgent)).toList(),
                            pageResult.getTotal(),
                            records.isEmpty() ? null : records.getLast().getId());
                });
    }


    /*
    ----------------------------------------------------------------
                        私有方法
    ----------------------------------------------------------------
     */

    /**
     * 实体转值对象
     */
    private SysOperateLogDTO convertEntity(SysOperateLogEntity entity) {
        if (entity == null) return null;

        var ret = new SysOperateLogDTO();
        BeanUtil.copyProperties(entity, ret);
        return ret;
    }

    /**
     * 实体转值对象
     */
    private List<SysOperateLogDTO> convertList(List<SysOperateLogEntity> entityList) {
        if (CollUtil.isEmpty(entityList)) {
            return List.of();
        }
        return auditNicknameFiller.fillByUserId(entityList, this::convertEntity);
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
