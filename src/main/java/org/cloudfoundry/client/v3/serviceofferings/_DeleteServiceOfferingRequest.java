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


import com.fasterxml.jackson.annotation.JsonIgnore;
import org.cloudfoundry.Nullable;
import org.cloudfoundry.QueryParameter;
import org.immutables.value.Value;

/**
 * The request payload for the Delete Service Offering operation.
 */
@Value.Immutable
abstract class _DeleteServiceOfferingRequest {

    /**
     * Whether any service plans, instances, and bindings associated with this service offering will also be deleted
     */
    @QueryParameter("purge")
    @Nullable
    abstract Boolean getPurge();

    /**
     * The service offering id
     */
    @JsonIgnore
    abstract String getServiceOfferingId();

}
