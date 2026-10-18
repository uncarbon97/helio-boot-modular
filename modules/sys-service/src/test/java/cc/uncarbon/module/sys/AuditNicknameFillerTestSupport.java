package cc.uncarbon.module.sys;

import cc.uncarbon.module.commons.facade.AuditNicknameFiller;
import cc.uncarbon.module.commons.model.valueobj.AuditUserDisplayAware;
import cc.uncarbon.module.commons.model.valueobj.AuditingUserSource;
import cc.uncarbon.module.commons.model.valueobj.OperateLogUserDisplayAware;
import cc.uncarbon.module.commons.model.valueobj.OperateLogUserSource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

/**
 * 测试用审计昵称填充器替身：仅按 converter 顺序完成转换，不查库、不填充昵称
 *
 * @author Uncarbon
 */
public final class AuditNicknameFillerTestSupport {

    private AuditNicknameFillerTestSupport() {
    }

    public static AuditNicknameFiller passThrough() {
        return new AuditNicknameFiller() {

            @Override
            public <E extends AuditingUserSource, T extends AuditUserDisplayAware> List<T> fill(
                    Collection<E> sources, Function<E, T> converter) {
                return convertAll(sources, converter);
            }

            @Override
            public <E extends OperateLogUserSource, T extends OperateLogUserDisplayAware> List<T> fillByUserId(
                    Collection<E> sources, Function<E, T> converter) {
                return convertAll(sources, converter);
            }

            private <E, T> List<T> convertAll(Collection<E> sources, Function<E, T> converter) {
                List<T> ret = new ArrayList<>(sources.size());
                for (E source : sources) {
                    ret.add(source == null ? null : converter.apply(source));
                }
                return ret;
            }
        };
    }
}
