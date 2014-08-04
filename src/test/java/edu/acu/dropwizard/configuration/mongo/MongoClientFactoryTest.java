/*
 * Copyright (c) 2014, Abilene Christian University
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright notice,
 *       this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright notice,
 *       this list of conditions and the following disclaimer in the documentation
 *       and/or other materials provided with the distribution.
 *     * Neither the name of Abilene Christian University nor the names of its
 *       contributors may be used to endorse or promote products derived from this
 *       software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package edu.acu.dropwizard.configuration.mongo;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.io.Resources;
import com.mongodb.DefaultDBDecoder;
import com.mongodb.DefaultDBEncoder;
import com.mongodb.LazyDBDecoder;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.ReadPreference;
import com.mongodb.ServerAddress;
import com.mongodb.WriteConcern;
import io.dropwizard.configuration.ConfigurationFactory;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.lifecycle.setup.LifecycleEnvironment;
import io.dropwizard.setup.Environment;
import java.io.File;
import java.util.Enumeration;
import javax.net.SocketFactory;
import javax.validation.Valid;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 * @author Harvey McQueen <hmcqueen at gmail.com>
 */
public class MongoClientFactoryTest {
    private File replicaTestFile;

    public static class Example {

        @JsonProperty
        @Getter
        @Valid
        @NotNull
        private MongoClientFactory mongoClient;

    }
    
    private final HealthCheckRegistry healthChecks = mock(HealthCheckRegistry.class);
    private final MetricRegistry metricRegistry = new MetricRegistry();
    private final LifecycleEnvironment lifecycleEnvironment = mock(LifecycleEnvironment.class);
    private final Environment environment = mock(Environment.class);
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    private final ConfigurationFactory<Example> factory
            = new ConfigurationFactory<>(Example.class, validator, Jackson.newObjectMapper(), "dw");
    private File testFile;
    private File optionsTestFile;

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
        when(environment.healthChecks()).thenReturn(healthChecks);
        when(environment.lifecycle()).thenReturn(lifecycleEnvironment);
        when(environment.metrics()).thenReturn(metricRegistry);
        
        this.testFile = new File(Resources.getResource("client-defaults-test.yml").toURI());
        this.optionsTestFile = new File(Resources.getResource("client-options-test.yml").toURI());
        this.replicaTestFile = new File(Resources.getResource("client-replica-test.yml").toURI());
    }

    // @Test  // This test requires mongodb to be running on localhost :(
    public void correctlyExtractsMongoClientFromConfiguration() throws Exception {
        final Example example = factory.build(testFile);
        final MongoClient client = example.getMongoClient().build(environment);

        assertThat(client.getAddress().getHost()).isIn("localhost", "127.0.0.1");
        assertThat(client.getAddress().getPort()).isEqualTo(ServerAddress.defaultPort());
        assertThat(client.getCredentialsList()).isEmpty();

        final MongoClientOptions options = client.getMongoClientOptions();

        assertThat(options.getDbDecoderFactory()).isEqualTo(DefaultDBDecoder.FACTORY);
        assertThat(options.getDbEncoderFactory()).isEqualTo(DefaultDBEncoder.FACTORY);
        assertThat(options.getReadPreference()).isEqualTo(ReadPreference.primary());
        assertThat(options.getWriteConcern()).isEqualTo(WriteConcern.ACKNOWLEDGED);
        assertThat(options.getSocketFactory()).isEqualTo(SocketFactory.getDefault());
    }

    // @Test  // This test requires mongodb to be running on localhost :(
    public void correctlyExtractsMongoClientWithOptionsFromConfiguration() throws Exception {
        final Example example = factory.build(optionsTestFile);
        final MongoClient client = example.getMongoClient().build(environment);

        assertThat(client).isNotNull();

        assertThat(client.getAddress().getHost()).isIn("localhost", "127.0.0.1");
        assertThat(client.getAddress().getPort()).isEqualTo(ServerAddress.defaultPort());
        assertThat(client.getCredentialsList()).isEmpty();

        final MongoClientOptions options = client.getMongoClientOptions();
        
        assertThat(options.getDescription()).isEqualTo("A MongoClient for the ages");
        assertThat(options.getMinConnectionsPerHost()).isEqualTo(42);
        assertThat(options.getConnectionsPerHost()).isEqualTo(84);
        assertThat(options.getThreadsAllowedToBlockForConnectionMultiplier()).isEqualTo(7);
        assertThat(options.getMaxWaitTime()).isEqualTo(1000 * 60 * 5);
        assertThat(options.getMaxConnectionIdleTime()).isEqualTo(30);
        assertThat(options.getMaxConnectionLifeTime()).isEqualTo(60);
        assertThat(options.getConnectTimeout()).isEqualTo(20000);
        assertThat(options.getSocketTimeout()).isEqualTo(1000 * 60 * 10);
        assertThat(options.isSocketKeepAlive()).isEqualTo(true);
        assertThat(options.getReadPreference()).isEqualTo(ReadPreference.secondaryPreferred());
        assertThat(options.getDbDecoderFactory()).isEqualTo(LazyDBDecoder.FACTORY);
        assertThat(options.getWriteConcern()).isEqualTo(WriteConcern.REPLICAS_SAFE);
        assertThat(options.isCursorFinalizerEnabled()).isEqualTo(false);
        assertThat(options.isAlwaysUseMBeans()).isEqualTo(true);
        assertThat(options.getHeartbeatFrequency()).isEqualTo(10000);
        assertThat(options.getHeartbeatConnectRetryFrequency()).isEqualTo(20);
        assertThat(options.getHeartbeatConnectTimeout()).isEqualTo(40000);
        assertThat(options.getHeartbeatSocketTimeout()).isEqualTo(40000);
        assertThat(options.getHeartbeatThreadCount()).isEqualTo(2);
        assertThat(options.getAcceptableLatencyDifference()).isEqualTo(30);

    }

    @Test
    public void correctlyExtractsMongoClientWithReplicaSetFromConfiguration() throws Exception {
        final Example example = factory.build(replicaTestFile);
        final MongoClient client = example.getMongoClient().build(environment);

        assertThat(client).isNotNull();
        
        assertThat(client.getAllAddress()).hasSize(2);
        
        final MongoClientOptions options = client.getMongoClientOptions();
        
        assertThat(options.getRequiredReplicaSetName()).isEqualTo("myRepSet1");
    }

}
