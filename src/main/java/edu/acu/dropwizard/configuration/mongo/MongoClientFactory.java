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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mongodb.*;
import io.dropwizard.setup.Environment;
import io.dropwizard.validation.ValidationMethod;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import com.mongodb.ServerAddress;
import java.net.InetSocketAddress;

/**
 *
 * @author Harvey McQueen <hmcqueen at gmail.com>
 */
public class MongoClientFactory {

    @RequiredArgsConstructor
    public enum DBDecoderFactoryType {

        LAZY(LazyDBDecoder.FACTORY),
        LAZY_WRITEABLE(LazyWriteableDBDecoder.FACTORY),
        DEFAULT(DefaultDBDecoder.FACTORY);

        @NonNull
        @Getter
        private final DBDecoderFactory value;

    }

    private static final String READ_PREFERENCE_KEY = "readPreference";
    private static final String WRITE_CONCERN_KEY = "writeConcern";
    private static final String DB_DECODER_FACTORY_KEY = "dbDecoderFactory";
    private static final Set<String> DB_DECODER_FACTORY_VALUES = Sets.newHashSet("lazy", "lazy-writeable", "default");

    @JsonProperty
    @JsonDeserialize(converter = ServerAddressConverter.class)
    @Getter
    private ServerAddress server = new ServerAddress(new InetSocketAddress(ServerAddress.defaultHost(), ServerAddress.defaultPort()));
    @JsonProperty
    @JsonDeserialize(contentConverter = ServerAddressConverter.class)
    @Getter
    private List<ServerAddress> servers = Lists.newArrayList();

    @JsonProperty
    @JsonDeserialize(contentConverter = MongoCredentialConverter.class)
    private List<MongoCredential> credentials = Lists.newArrayList();

    @JsonProperty
    private Map<String, String> options = Maps.newHashMap();
    
    public MongoClientFactory() {
        
    }

    public MongoClient build(Environment environment) {
        MongoClientOptions.Builder optionsBuilder = new MongoClientOptions.Builder();
        for (Map.Entry<String, String> entry : options.entrySet()) {
            setOption(optionsBuilder, entry.getKey(), entry.getValue());
        }
        
        MongoClient client;

        if (servers.size() > 0) {
            client = new MongoClient(servers, credentials, optionsBuilder.build());
        } else {
            client = new MongoClient(server, credentials, optionsBuilder.build());
        }
        
        return client;
    }

    @JsonIgnore
    @ValidationMethod(message = ".options.readPreference must be one of \"primary\", \"primaryPreferred\", \"secondary\", \"secondaryPreferred\", or \"nearest\"")
    public boolean isReadPreferenceValid() {
        if (options.get(READ_PREFERENCE_KEY) == null) {
            return true;
        }
        try {
            ReadPreference.valueOf(options.get(READ_PREFERENCE_KEY));
        } catch (IllegalArgumentException | ClassCastException e) {
            return false;
        }
        return true;
    }

    @JsonIgnore
    @ValidationMethod(message = ".options.writeConcern must be one of the static WriteConcerns on com.mongodb.WriteConcern by name.  Case doesn't matter.")
    public boolean isWriteConcernValid() {
        return options.get(WRITE_CONCERN_KEY) == null || WriteConcern.valueOf(options.get(WRITE_CONCERN_KEY)) != null;
    }

    @JsonIgnore
    @ValidationMethod(message = ".options.dbDecoderFactory must be one of \"lazy\", \"lazy-writeable\", or \"default\"")
    public boolean isDBDecoderFactoryValid() {
        return options.get(DB_DECODER_FACTORY_KEY) == null || DB_DECODER_FACTORY_VALUES.contains(options.get(DB_DECODER_FACTORY_KEY));
    }

    private static void setOption(MongoClientOptions.Builder options, String propertyName, String propertyValue) {
        switch (propertyName) {
            case DB_DECODER_FACTORY_KEY:
                switch (propertyValue) {
                    case "lazy":
                        options.dbDecoderFactory(LazyDBDecoder.FACTORY);
                        break;
                    case "lazy-writeable":
                        options.dbDecoderFactory(LazyWriteableDBDecoder.FACTORY);
                        break;
                    case "default":
                        options.dbDecoderFactory(DefaultDBDecoder.FACTORY);
                        break;
                    default:
                        // Handled by validation
                        break;
                }
                break;
            case "readPreference":
                try {
                    options.readPreference(ReadPreference.valueOf(propertyValue));
                } catch (IllegalArgumentException e) {
                    // This shouldn't matter since we already validated
                }
                break;
            case "writeConcern":
                try {
                    options.writeConcern(WriteConcern.valueOf(propertyValue));
                } catch (IllegalArgumentException e) {
                    // This shouldn't matter since we already validated
                }
                break;
            default:
                try {
                    Method m = MongoClientOptions.Builder.class.getMethod(propertyName, int.class);
                    m.invoke(options, Integer.parseInt(propertyValue));
                } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                }
                try {
                    Method m = MongoClientOptions.Builder.class.getMethod(propertyName, boolean.class);
                    m.invoke(options, Boolean.parseBoolean(propertyValue));
                } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                }
                try {
                    Method m = MongoClientOptions.Builder.class.getMethod(propertyName, String.class);
                    m.invoke(options, propertyValue);
                } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                }
                try {
                    Method m = MongoClientOptions.Builder.class.getMethod(propertyName, long.class);
                    m.invoke(options, Long.parseLong(propertyValue));
                } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                }
                break;
        }
    }

}
