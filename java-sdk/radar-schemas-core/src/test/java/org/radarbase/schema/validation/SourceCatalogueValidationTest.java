/*
 * Copyright 2017 King's College London and The Hyve
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.radarbase.schema.validation;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.radarbase.schema.specification.SourceCatalogue.BASE_PATH;
import static org.radarbase.schema.validation.ValidationHelper.isValidTopic;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.lang.reflect.Executable;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opentest4j.MultipleFailuresError;
import org.radarbase.schema.specification.DataProducer;
import org.radarbase.schema.specification.SourceCatalogue;

/**
 * TODO.
 */
public class SourceCatalogueValidationTest {
    private static SourceCatalogue catalogue;

    @BeforeAll
    public static void setUp() throws IOException {
        catalogue = SourceCatalogue.load(BASE_PATH);
    }

    @Test
    public void validateTopicNames() {
        catalogue.getTopicNames().forEach(topic ->
                assertTrue(isValidTopic(topic), topic + " is invalid"));
    }

    @Test
    public void validateTopics() {
        List<String> expected = Stream.of(
                catalogue.getActiveSources(),
                catalogue.getMonitorSources(),
                catalogue.getPassiveSources(),
                catalogue.getStreamGroups(),
                catalogue.getConnectorSources(),
                catalogue.getPushSources())
                .flatMap(map -> map.values().stream())
                .flatMap(DataProducer::getTopicNames)
                .sorted()
                .collect(Collectors.toList());

        assertEquals(expected, catalogue.getTopicNames().sorted().collect(Collectors.toList()));
    }

    @Test
    public void validateTopicSchemas() {
        catalogue.getSources().stream()
                .flatMap(source -> source.getData().stream())
                .forEach(data -> {
                    try {
                        assertTrue(data.getTopics(catalogue.getSchemaCatalogue())
                                .findAny().isPresent());
                    } catch (IOException ex) {
                        fail("Cannot create topic from specification: " + ex);
                    }
                });
    }

    @Test
    public void validateSerialization() {
        ObjectMapper mapper = new ObjectMapper();

        List<IllegalArgumentException> failures = catalogue.getSources()
                .stream()
                .map(source -> {
                    try {
                        String json = mapper.writeValueAsString(source);
                        assertFalse(json.contains("\"parallel\":false"));
                        return null;
                    } catch (Exception ex) {
                        return new IllegalArgumentException("Source " + source + " is not valid", ex);
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (!failures.isEmpty()) {
            throw new MultipleFailuresError("One or more sources were not valid", failures);
        }
    }
}