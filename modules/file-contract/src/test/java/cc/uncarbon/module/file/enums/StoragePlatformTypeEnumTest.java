package cc.uncarbon.module.file.enums;

import cc.uncarbon.framework.helium.base.enums.BaseEnum;
import cc.uncarbon.module.commons.enumdict.EnumDict;
import cc.uncarbon.module.file.model.setting.storage.AliyunOssSetting;
import cc.uncarbon.module.file.model.setting.storage.AmazonS3Setting;
import cc.uncarbon.module.file.model.setting.storage.AmazonS3V2Setting;
import cc.uncarbon.module.file.model.setting.storage.LocalSetting;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Optional;

/**
 * {@link StoragePlatformTypeEnum} 单元测试
 */
class StoragePlatformTypeEnumTest {

    /**
     * 已知常量的取值与标签应一一对应
     */
    @Test
    void knownConstantsValuesAndLabels() {
        Assertions.assertEquals("local", StoragePlatformTypeEnum.LOCAL.getValue());
        Assertions.assertEquals("本地", StoragePlatformTypeEnum.LOCAL.getLabel());
        Assertions.assertEquals("s3", StoragePlatformTypeEnum.AMAZON_S3.getValue());
        Assertions.assertEquals("S3 兼容", StoragePlatformTypeEnum.AMAZON_S3.getLabel());
        Assertions.assertEquals("s3-v2", StoragePlatformTypeEnum.AMAZON_S3_V2.getValue());
        Assertions.assertEquals("S3-v2 兼容", StoragePlatformTypeEnum.AMAZON_S3_V2.getLabel());
        Assertions.assertEquals("aliyun_oss", StoragePlatformTypeEnum.ALIYUN_OSS.getValue());
        Assertions.assertEquals("阿里云 OSS", StoragePlatformTypeEnum.ALIYUN_OSS.getLabel());
        Assertions.assertEquals(4, StoragePlatformTypeEnum.values().length);
    }

    /**
     * 所有常量的取值与标签均应为非空文本
     */
    @Test
    void everyConstantHasNonBlankValueAndLabel() {
        for (StoragePlatformTypeEnum item : StoragePlatformTypeEnum.values()) {
            Assertions.assertFalse(item.getValue().isBlank());
            Assertions.assertFalse(item.getLabel().isBlank());
        }
    }

    /**
     * 每个常量应绑定对应的存储设置类
     */
    @Test
    void everyConstantBindsSettingClass() {
        Assertions.assertSame(LocalSetting.class, StoragePlatformTypeEnum.LOCAL.getSettingClass());
        Assertions.assertSame(AmazonS3Setting.class, StoragePlatformTypeEnum.AMAZON_S3.getSettingClass());
        Assertions.assertSame(AmazonS3V2Setting.class, StoragePlatformTypeEnum.AMAZON_S3_V2.getSettingClass());
        Assertions.assertSame(AliyunOssSetting.class, StoragePlatformTypeEnum.ALIYUN_OSS.getSettingClass());
    }

    /**
     * BaseEnum.of 应支持所有常量按 value 往返反查
     */
    @Test
    void baseEnumOfRoundTripForAllConstants() {
        for (StoragePlatformTypeEnum item : StoragePlatformTypeEnum.values()) {
            Optional<StoragePlatformTypeEnum> hit = BaseEnum.of(StoragePlatformTypeEnum.class, item.getValue());
            Assertions.assertTrue(hit.isPresent(), item + " 应能按 value 反查");
            Assertions.assertSame(item, hit.get());
        }
    }

    /**
     * BaseEnum.of 对未启用的平台取值应返回空 Optional
     */
    @Test
    void baseEnumOfReturnsEmptyForUnknownValue() {
        Assertions.assertFalse(BaseEnum.of(StoragePlatformTypeEnum.class, "ftp").isPresent());
    }

    /**
     * 枚举应标注 @EnumDict 且名称为「文件存储平台」
     */
    @Test
    void annotatedAsEnumDict() {
        EnumDict dict = StoragePlatformTypeEnum.class.getAnnotation(EnumDict.class);
        Assertions.assertNotNull(dict);
        Assertions.assertEquals("文件存储平台", dict.name());
    }
}
