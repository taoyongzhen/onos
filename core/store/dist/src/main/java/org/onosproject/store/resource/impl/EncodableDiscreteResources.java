/*
 * Copyright 2016-present Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.store.resource.impl;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Sets;
import org.onosproject.net.resource.DiscreteResource;
import org.onosproject.net.resource.DiscreteResourceCodec;
import org.onosproject.net.resource.DiscreteResourceId;
import org.onosproject.net.resource.Resources;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A set of discrete resources that can be encoded as integers.
 */
final class EncodableDiscreteResources implements DiscreteResources {
    private static final Codecs CODECS = Codecs.getInstance();
    private final DiscreteResource parent;
    private final Map<Class<?>, EncodedDiscreteResources> values;

    private static Class<?> getClass(DiscreteResource resource) {
        return resource.valueAs(Object.class).map(Object::getClass).get();
    }

    static DiscreteResources of(Set<DiscreteResource> resources) {
        if (resources.isEmpty()) {
            return DiscreteResources.empty();
        }

        DiscreteResource parent = resources.iterator().next().parent().get();
        return of(parent, resources);
    }

    static EncodableDiscreteResources of(DiscreteResource parent, Set<DiscreteResource> resources) {
        Map<Class<?>, Set<DiscreteResource>> grouped = resources.stream()
                .collect(Collectors.groupingBy(x -> getClass(x), Collectors.toCollection(LinkedHashSet::new)));

        Map<Class<?>, EncodedDiscreteResources> values = new LinkedHashMap<>();
        for (Map.Entry<Class<?>, Set<DiscreteResource>> entry : grouped.entrySet()) {
            DiscreteResourceCodec<?> codec = CODECS.getCodec(entry.getKey());
            values.put(entry.getKey(), EncodedDiscreteResources.of(entry.getValue(), codec));
        }

        return new EncodableDiscreteResources(parent, values);
    }

    private EncodableDiscreteResources(DiscreteResource parent, Map<Class<?>, EncodedDiscreteResources> values) {
        this.parent = parent;
        this.values = values;
    }

    // for serializer
    private EncodableDiscreteResources() {
        this.parent = null;
        this.values = null;
    }

    @Override
    public Optional<DiscreteResource> lookup(DiscreteResourceId id) {
        DiscreteResource resource = Resources.discrete(id).resource();
        Class<?> cls = getClass(resource);
        return Optional.ofNullable(values.get(cls))
                .filter(x -> x.contains(resource))
                .map(x -> resource);
    }

    @Override
    public DiscreteResources difference(DiscreteResources other) {
        return of(parent, Sets.difference(values(), other.values()));
    }

    @Override
    public boolean isEmpty() {
        return values.values().stream()
                .allMatch(x -> x.isEmpty());
    }

    @Override
    public boolean containsAny(Set<DiscreteResource> other) {
        return !Sets.intersection(this.values(), other).isEmpty();
    }

    @Override
    public DiscreteResources add(DiscreteResources other) {
        Set<DiscreteResource> union = Sets.union(values(), other.values());

        return of(parent, union);
    }

    @Override
    public Set<DiscreteResource> values() {
        return values.values().stream()
                .flatMap(x -> x.values(parent.id()).stream())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    DiscreteResource parent() {
        return parent;
    }

    Map<Class<?>, EncodedDiscreteResources> rawValues() {
        return values;
    }

    @Override
    public int hashCode() {
        return Objects.hash(parent, values);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final EncodableDiscreteResources other = (EncodableDiscreteResources) obj;
        return Objects.equals(this.parent, other.parent)
                && Objects.equals(this.values, other.values);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("values", values())
                .toString();
    }
}
