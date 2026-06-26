package cc.uncarbon.module.sys.service.impl;

import cc.uncarbon.module.commons.resoler.IPLocationResolver;
import cc.uncarbon.module.sys.dal.entity.SysOperateLogEntity;
import cc.uncarbon.module.sys.dal.mapper.SysOperateLogMapper;
import cc.uncarbon.module.sys.model.request.SysOperateLogCreateRequest;
import cc.uncarbon.module.sys.service.SysOperateLogService;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.text.CharSequenceUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

}
