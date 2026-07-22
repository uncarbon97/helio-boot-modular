package cc.uncarbon.module.support;

import cc.uncarbon.framework.helium.db.enums.EnabledStatusEnum;
import cc.uncarbon.framework.helium.db.enums.GenderEnum;
import cc.uncarbon.framework.helium.db.enums.YesOrNoEnum;
import cc.uncarbon.module.commons.enumdict.EnumDictContributor;
import cc.uncarbon.module.commons.enumdict.EnumDictSpec;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 手动追加系统字典
 *
 * @author Uncarbon
 */
@Configuration
public class AppendSysDictContributor implements EnumDictContributor {

    @Override
    public Collection<EnumDictSpec> contribute() {
        List<EnumDictSpec> ret = new ArrayList<>();

        /*
        框架内置枚举
         */
        ret.add(EnumDictSpec.of("enabled_status", "启用状态枚举", null,
                EnabledStatusEnum.class, EnabledStatusEnum::getValue, EnabledStatusEnum::getLabel));
        ret.add(EnumDictSpec.of("gender", "生理性别枚举", null,
                GenderEnum.class, GenderEnum::getValue, GenderEnum::getLabel));
        ret.add(EnumDictSpec.of("yes_or_no", "是或否枚举", null,
                YesOrNoEnum.class, YesOrNoEnum::getValue, YesOrNoEnum::getLabel));

        return ret;
    }
}
