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

import org.cloudfoundry.Nullable;
import org.cloudfoundry.QueryParameter;
import org.cloudfoundry.client.v3.FilterParameter;
import org.cloudfoundry.client.v3.PaginatedRequest;
import org.immutables.value.Value;

import java.util.List;

/**
 * The request payload for the List Service Offerings operation
 */
@Value.Immutable
abstract class _ListServiceOfferingsRequest extends PaginatedRequest {

    /**
     * Whether the service offering is available
     */
    @FilterParameter("available")
    @Nullable
    abstract Boolean getAvailable();

    /**
     * List of label selectors
     */
    @QueryParameter("label_selector")
    @Nullable
    abstract List<String> getLabelSelector();

    /**
     * List of names to filter by
     */
    @FilterParameter("names")
    @Nullable
    abstract List<String> getNames();

    /**
     * List of organization ids to filter by
     */
    @FilterParameter("organization_guids")
    @Nullable
    abstract List<String> getOrganizationIds();

    /**
     * List of service broker ids to filter by
     */
    @FilterParameter("service_broker_guids")
    @Nullable
    abstract List<String> getServiceBrokerIds();

    /**
     * List of service broker names to filter by
     */
    @FilterParameter("service_broker_names")
    @Nullable
    abstract List<String> getServiceBrokerNames();

    /**
     * List of space ids to filter by
     */
    @FilterParameter("space_guids")
    @Nullable
    abstract List<String> getSpaceIds();

}
