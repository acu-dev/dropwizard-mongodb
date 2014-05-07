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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import com.mongodb.MongoCredential;
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

/**
 *
 * @author Harvey McQueen <hmcqueen at gmail.com>
 */
public class MongoCredentialConverterTest {
    
    public static class Example {
        
        @JsonProperty
        @JsonDeserialize(contentConverter = MongoCredentialConverter.class)
        @Getter
        private List<MongoCredential> credentials = Lists.newArrayList();

    }
    
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    private final ConfigurationFactory<MongoCredentialConverterTest.Example> factory
            = new ConfigurationFactory<>(MongoCredentialConverterTest.Example.class, validator, Jackson.newObjectMapper(), "dw");
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
        this.testFile = new File(Resources.getResource("credential-test.yml").toURI());
    }
    
    @Test
    public void correctlyExtractsMongoCredentialsFromConfiguration() throws Exception {
        final MongoCredentialConverterTest.Example example = factory.build(testFile);
        
        assertThat(example.getCredentials()).hasSize(4);
        
        // CR
        assertThat(example.getCredentials().get(0).getUserName()).isEqualTo("nope");
        assertThat(example.getCredentials().get(0).getSource()).isEqualTo("yep");
        assertThat(example.getCredentials().get(0).getPassword()).isEqualTo("maybe".toCharArray());
        assertThat(example.getCredentials().get(0).getMechanism()).isEqualTo(MongoCredential.MONGODB_CR_MECHANISM);
        
        // x509
        assertThat(example.getCredentials().get(1).getUserName()).isEqualTo("cn=blee,dc=somewhere");
        assertThat(example.getCredentials().get(1).getSource()).isEqualTo("$external");
        assertThat(example.getCredentials().get(1).getPassword()).isEqualTo(null);
        assertThat(example.getCredentials().get(1).getMechanism()).isEqualTo(MongoCredential.MONGODB_X509_MECHANISM);
        
        // Plain
        assertThat(example.getCredentials().get(2).getUserName()).isEqualTo("me");
        assertThat(example.getCredentials().get(2).getSource()).isEqualTo("yeppers");
        assertThat(example.getCredentials().get(2).getPassword()).isEqualTo("excellent".toCharArray());
        assertThat(example.getCredentials().get(2).getMechanism()).isEqualTo(MongoCredential.PLAIN_MECHANISM);
        
        // GSSAPI
        assertThat(example.getCredentials().get(3).getUserName()).isEqualTo("user");
        assertThat(example.getCredentials().get(3).getSource()).isEqualTo("$external");
        assertThat(example.getCredentials().get(3).getPassword()).isEqualTo(null);
        assertThat(example.getCredentials().get(3).getMechanism()).isEqualTo(MongoCredential.GSSAPI_MECHANISM);
        assertThat(example.getCredentials().get(3).getMechanismProperty("SERVICE_NAME", "failed")).isEqualTo("custom");
        assertThat(example.getCredentials().get(3).getMechanismProperty("CANONICALIZE_HOST_NAME", false)).isEqualTo(true);

    }
    
}
