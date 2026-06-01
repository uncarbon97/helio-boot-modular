package cc.uncarbon.module.tenant.service.impl;

import cc.uncarbon.module.tenant.dal.entity.TenantPackageEntity;
import cc.uncarbon.module.tenant.dal.mapper.TenantPackageMapper;
import cc.uncarbon.module.tenant.service.TenantPackageService;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.List;


/**
 * 租户套餐
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class TenantPackageServiceImpl implements TenantPackageService {

    private final TenantPackageMapper tenantPackageMapper;


    @Override
    public TenantPackageEntity getById(Long id) {
        return tenantPackageMapper.selectById(id);
    }

    @Override
    public List<TenantPackageEntity> listByIds(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        return tenantPackageMapper.selectBatchIds(ids);
    }

    @Override
    public void deleteByIds(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return;
        }
        tenantPackageMapper.deleteBatchIds(ids);
    }
}
