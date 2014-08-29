/*
 * Mineotaur: a visual analytics tool for high-throughput microscopy screens
 * Copyright (C) 2014  Bálint Antal (University of Cambridge)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mineotor.application;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import java.util.logging.Logger;

/**
 * Created by balintantal on 28/05/2014.
 */
@ComponentScan(basePackages = {"org.org.mineotor.controller", "org.org.mineotor.application"})
@EnableAutoConfiguration
public class Mineotor extends WebMvcConfigurerAdapter {

    public static Logger LOGGER = Logger.getLogger(Mineotor.class.getName());

    public static void main(String[] args) {
        SpringApplication.run(Mineotor.class, args);
    }

}
