package cc.uncarbon.module.commons.enumdict;

import java.util.Collection;

/**
 * 方便手动添加来自框架包外的枚举作为内置字典
 *
 * @author Uncarbon
 */
public interface EnumDictContributor {

    Collection<EnumDictSpec> contribute();

}
