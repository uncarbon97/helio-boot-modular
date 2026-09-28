package cc.uncarbon.module.tenant.service.impl;

import cc.uncarbon.module.tenant.dal.entity.TenantPackageMenuRelationEntity;
import cc.uncarbon.module.tenant.dal.mapper.TenantPackageMenuRelationMapper;
import cc.uncarbon.module.tenant.MybatisPlusTestSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

/**
 * {@link TenantPackageMenuRelationServiceImpl} 增量绑定分支测试
 */
@ExtendWith(MockitoExtension.class)
class TenantPackageMenuRelationServiceImplTest {

    @BeforeAll
    static void initTableInfoCache() {
        MybatisPlusTestSupport.initTableInfo();
    }

    @Mock
    private TenantPackageMenuRelationMapper mapper;

    @InjectMocks
    private TenantPackageMenuRelationServiceImpl service;


    @Test
    void listMenuIdsByPackageDelegates() {
        Mockito.when(mapper.listMenuIdsByPackage(5L)).thenReturn(List.of(1L, 2L));

        Assertions.assertEquals(List.of(1L, 2L), service.listMenuIdsByPackage(5L));
    }

    @Test
    void cleanAndBindEmptyDeletesAll() {
        service.cleanAndBind(5L, Set.of());

        Mockito.verify(mapper).delete(Mockito.any());
        Mockito.verify(mapper, Mockito.never()).insert(Mockito.anyList());
    }

    @Test
    void cleanAndBindAppendsOnlyMissing() {
        Mockito.when(mapper.selectList(Mockito.any()))
                .thenReturn(List.of(TenantPackageMenuRelationEntity.of(5L, 1L)))
                .thenReturn(List.of());

        service.cleanAndBind(5L, Set.of(1L, 2L));

        Mockito.verify(mapper).delete(Mockito.any());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<TenantPackageMenuRelationEntity>> captor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(mapper).insert(captor.capture());
        Assertions.assertEquals(1, captor.getValue().size());
        Assertions.assertEquals(2L, captor.getValue().get(0).getMenuId());
        Assertions.assertEquals(5L, captor.getValue().get(0).getPackageId());
    }
}
