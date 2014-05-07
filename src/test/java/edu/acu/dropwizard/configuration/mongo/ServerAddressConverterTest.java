/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.acu.dropwizard.configuration.mongo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import com.mongodb.ServerAddress;
import io.dropwizard.configuration.ConfigurationFactory;
import io.dropwizard.jackson.Jackson;
import java.io.File;
import java.util.Enumeration;
import java.util.List;
import javax.validation.Validation;
import javax.validation.Validator;
import lombok.Getter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.failBecauseExceptionWasNotThrown;

/**
 *
 * @author Harvey McQueen <hmcqueen at gmail.com>
 */
public class ServerAddressConverterTest {

    public static class Example {

        @JsonProperty
        @JsonDeserialize(converter = ServerAddressConverter.class)
        @Getter
        private ServerAddress server = null;
        @JsonProperty
        @JsonDeserialize(converter = ServerAddressConverter.class)
        @Getter
        private ServerAddress server2 = null;
        @JsonProperty
        @JsonDeserialize(converter = ServerAddressConverter.class)
        @Getter
        private ServerAddress server3 = null;
        @JsonProperty
        @JsonDeserialize(contentConverter = ServerAddressConverter.class)
        @Getter
        private List<ServerAddress> servers = Lists.newArrayList();

    }

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    private final ConfigurationFactory<Example> factory
            = new ConfigurationFactory<>(Example.class, validator, Jackson.newObjectMapper(), "dw");
    private File testFile;

    @After
    public void resetConfigOverrides() {
        for (Enumeration<?> props = System.getProperties().propertyNames(); props.hasMoreElements();) {
            String keyString = (String) props.nextElement();
            if (keyString.startsWith("dw.")) {
                System.clearProperty(keyString);
            }
        }
    }

    @Before
    public void setUp() throws Exception {
        this.testFile = new File(Resources.getResource("server-test.yml").toURI());
    }

    @Test
    public void correctlyExtractsServerAddressesFromConfiguration() throws Exception {
        final Example example = factory.build(testFile);
        
        assertThat(example.getServer().getHost()).isEqualTo("test");
        assertThat(example.getServer().getPort()).isEqualTo(27017);
        
        assertThat(example.getServer2().getHost()).isEqualTo("127.0.0.1");
        assertThat(example.getServer2().getPort()).isEqualTo(35403);
        
        assertThat(example.getServer3().getHost()).isEqualTo("blee");
        assertThat(example.getServer3().getPort()).isEqualTo(27018);
        
        assertThat(example.getServers()).hasSize(3);
        assertThat(example.getServers().get(0).getHost()).isEqualTo("test");
        assertThat(example.getServers().get(0).getPort()).isEqualTo(27017);

    }

}
