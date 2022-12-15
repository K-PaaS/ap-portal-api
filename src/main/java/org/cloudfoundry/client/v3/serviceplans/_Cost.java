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

package org.cloudfoundry.client.v3.serviceplans;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

/**
 * Cost data for a Service Plan
 */
@JsonDeserialize
@Value.Immutable
abstract class _Cost {

    /**
     * Pricing amount
     */
    @JsonProperty("amount")
    abstract Float getAmount();

    /**
     * Currency code for the pricing amount
     */
    @JsonProperty("currency")
    abstract String getCurrency();

    /**
     * Display name for type of cost, e.g. Monthly, Hourly, Request, GB
     */
    @JsonProperty("unit")
    abstract String getUnit();

}
