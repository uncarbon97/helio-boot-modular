package cc.uncarbon.module.sys.service.impl;

import cc.uncarbon.framework.helium.base.page.PageResult;
import cc.uncarbon.framework.helium.web.context.VisitorContext;
import cc.uncarbon.module.commons.exception.NoRecordException;
import cc.uncarbon.module.commons.resoler.IPLocationResolver;
import cc.uncarbon.module.sys.dal.entity.SysLoginLogEntity;
import cc.uncarbon.module.sys.dal.mapper.SysLoginLogMapper;
import cc.uncarbon.module.sys.model.query.AdminSysLoginLogListQuery;
import cc.uncarbon.module.sys.model.request.SysLoginLogCreateRequest;
import cc.uncarbon.module.sys.model.valueobj.SysLoginLogDTO;
import cc.uncarbon.module.sys.service.SysLoginLogService;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;


/**
 * 系统登录日志
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class SysLoginLogServiceImpl implements SysLoginLogService {

    /**
     * UA可以接受的最大长度
     */
    public static final int USER_AGENT_MAX_LENGTH = 255;

    private final SysLoginLogMapper sysLoginLogMapper;
    private final IPLocationResolver ipLocationResolver;


    /**
     * 后台管理-分页查询
     */
    @Override
    public PageResult<SysLoginLogDTO> adminList(AdminSysLoginLogListQuery query) {
        Page<SysLoginLogEntity> entityPage = sysLoginLogMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()),
                new LambdaQueryWrapper<SysLoginLogEntity>()
                        // 用户账号
                        .like(CharSequenceUtil.isNotBlank(query.getUserPin()), SysLoginLogEntity::getUserPin, CharSequenceUtil.cleanBlank(query.getUserPin()))
                        // 状态
                        .eq(Objects.nonNull(query.getResultStatus()), SysLoginLogEntity::getResultStatus, query.getResultStatus())
                        // 时间区间
                        .between(Objects.nonNull(query.getBeginAt()) && Objects.nonNull(query.getEndAt()), SysLoginLogEntity::getCreatedAt, query.getBeginAt(), query.getEndAt())
                        // 排序
                        .orderByDesc(SysLoginLogEntity::getId)
        );

        return this.convertPage(entityPage);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Long create(SysLoginLogCreateRequest request) {
        SysLoginLogEntity entity = new SysLoginLogEntity();
        BeanUtil.copyProperties(request, entity);
        // 按需改写字段
        VisitorContext visitorContext = request.getVisitorContext();
        String ua = visitorContext.getUserAgent();
        if (CharSequenceUtil.length(ua) > USER_AGENT_MAX_LENGTH) {
            // 超长度截断
            ua = CharSequenceUtil.subPre(ua, USER_AGENT_MAX_LENGTH);
        }

        String ipLocation = null;
        if (Objects.nonNull(visitorContext.getIp())) {
            ipLocation = ipLocationResolver.resolve(visitorContext.getIp());
        }

        entity
                .setVisitorIp(visitorContext.getIp())
                .setVisitorUserAgent(ua)
                .setVisitorIpLocation(ipLocation);

        sysLoginLogMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public SysLoginLogDTO getById(Long id) {
        SysLoginLogEntity entity = sysLoginLogMapper.selectById(id);
        return convertEntity(entity);
    }

    @Override
    public SysLoginLogDTO getNonnullById(Long id) throws NoRecordException {
        return NoRecordException.throwIfNull(getById(id));
    }

    /*
    ----------------------------------------------------------------
                        私有方法 private methods
    ----------------------------------------------------------------
     */

    /**
     * 实体转值对象
     */
    private SysLoginLogDTO convertEntity(SysLoginLogEntity entity) {
        if (entity == null) {
            return null;
        }

        SysLoginLogDTO ret = new SysLoginLogDTO();
        BeanUtil.copyProperties(entity, ret);
        // 按需改写字段
        UserAgent parsedUa = UserAgentUtil.parse(entity.getVisitorUserAgent());
        ret
                .setVisitorBrowser("%s %s".formatted(parsedUa.getBrowser().getName(), parsedUa.getVersion()))
                .setVisitorOs("%s %s".formatted(parsedUa.getOs().getName(), parsedUa.getOsVersion()));

        return ret;
    }

    /**
     * 实体转值对象
     */
    private List<SysLoginLogDTO> convertList(List<SysLoginLogEntity> entityList) {
        if (CollUtil.isEmpty(entityList)) {
            return List.of();
        }
        return entityList.stream().map(this::convertEntity).toList();
    }

    /**
     * 实体转值对象
     */
    private PageResult<SysLoginLogDTO> convertPage(Page<SysLoginLogEntity> entityPage) {
        return new PageResult<SysLoginLogDTO>()
                .setCurrent(entityPage.getCurrent())
                .setSize(entityPage.getSize())
                .setTotal(entityPage.getTotal())
                .setRecords(convertList(entityPage.getRecords()));
    }

}
