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

package org.mineotaur.application;

import org.apache.catalina.Context;
import org.apache.catalina.connector.Connector;
import org.mineotaur.provider.GenericEmbeddedGraphDatabaseProvider;
import org.mineotaur.provider.GraphDatabaseProvider;
import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.boot.context.embedded.tomcat.TomcatContextCustomizer;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Config for Mineotaur.
 */
@Configuration
public class MineotaurConfig {

    /**
     * Injects a provider instance.
     * @return The GraphDatabaseProvider instance.
     */
    @Bean(name = "provider")
    public GraphDatabaseProvider graphDatabaseProvider() {
        return new GenericEmbeddedGraphDatabaseProvider();
    }

    /**
     * Sets servlet container properties.
     * @return the EmbeddedServletContainerFactory instance.
     */
    @Bean
    public EmbeddedServletContainerFactory servletContainer() {
        TomcatEmbeddedServletContainerFactory factory = new TomcatEmbeddedServletContainerFactory();
        factory.addConnectorCustomizers(new TomcatConnectorCustomizer() {
            @Override
            public void customize(Connector connector) {
                connector.setAttribute("maxHttpHeaderSize", 4096000);
                connector.setAttribute("compression", "on");
                connector.setAttribute("useSendfile",false);
                connector.setAttribute("compressableMimeType","text/html,text/xml,text/plain,text/css,text/javascript,text/json,application/x-javascript,application/javascript,application/json");
            }
        });
        factory.addContextCustomizers(new TomcatContextCustomizer() {
            @Override
            public void customize(Context context) {
                context.setPath("/mineotaur");
            }
        });
        return factory;
    }

    @Bean
    public EmbeddedServletContainerCustomizer containerCustomizer() {
        return new EmbeddedServletContainerCustomizer() {
            @Override
            public void customize(ConfigurableEmbeddedServletContainer configurableEmbeddedServletContainer) {
                configurableEmbeddedServletContainer.setContextPath("/mineotaur");
            }
        };
    }






}
