package cc.uncarbon.module.file.config;

import cc.uncarbon.module.file.support.DynamicFileStorageRegistrar;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.dromara.x.file.storage.spring.EnableFileStorage;
import org.springframework.context.annotation.Configuration;


@EnableFileStorage
@RequiredArgsConstructor
@Configuration
public class FileStorageConfiguration {

    private final DynamicFileStorageRegistrar  dynamicFileStorageRegistrar;

    @PostConstruct
    public void init() {
        // 全量注册DB存储点
        dynamicFileStorageRegistrar.reloadAll();
    }
}
