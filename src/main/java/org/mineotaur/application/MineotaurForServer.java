package org.mineotaur.application;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.web.SpringBootServletInitializer;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

/**
 * Created by Balint on 2015-09-21.
 */
@ComponentScan(basePackages = {"org.mineotaur.controller", "org.mineotaur.application"})
@EnableAutoConfiguration
@PropertySource("classpath:application.properties")
public class MineotaurForServer extends SpringBootServletInitializer {

    @Autowired
    private static Environment env;

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(MineotaurForServer.class);
    }

    public static void main(String[] args) throws Exception {
        Mineotaur.name = env.getProperty("mineotaur.name");
        SpringApplication.run(MineotaurForServer.class, args);
    }
}
