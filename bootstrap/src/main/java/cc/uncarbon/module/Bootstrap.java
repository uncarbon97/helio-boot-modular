package cc.uncarbon.module;

import cc.uncarbon.framework.crud.annotation.EnableInitHikariPoolAtStartup;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


/**
 * @author Uncarbon
 */
@EnableInitHikariPoolAtStartup
@SpringBootApplication
public class Bootstrap {
    public static void main(String[] args) {
        SpringApplication.run(Bootstrap.class, args);
    }

}
