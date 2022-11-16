/*
 * Copyright 2013-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.cloudfoundry.client.v3.serviceofferings;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.cloudfoundry.AllowNulls;
import org.cloudfoundry.Nullable;
import org.immutables.value.Value;

import java.util.Map;

/**
 * Broker Catalog information for a Service Offering
 */
@JsonDeserialize
@Value.Immutable
abstract class _BrokerCatalog {

    /**
     * The identifier that the service broker provided for this service offering
     */
    @JsonProperty("id")
    abstract String getBrokerCatalogId();

    /**
     * Broker-related features of the service offering
     */
    @JsonProperty("features")
    abstract Features getFeatures();

    /**
     * The metadata
     */
    @AllowNulls
    @JsonProperty("metadata")
    @Nullable
    abstract Map<String, Object> getMetadata();

}
