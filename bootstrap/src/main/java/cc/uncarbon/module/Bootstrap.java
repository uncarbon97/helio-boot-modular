package cc.uncarbon.module;

import cc.uncarbon.module.appapi.props.AppApiProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;


/**
 * @author Uncarbon
 */
@EnableConfigurationProperties(value = {AppApiProperties.class})
@SpringBootApplication
public class Bootstrap {
    static void main(String[] args) {
        SpringApplication.run(Bootstrap.class, args);
    }

}
