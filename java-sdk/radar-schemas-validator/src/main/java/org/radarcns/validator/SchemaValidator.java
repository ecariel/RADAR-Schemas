package org.radarcns.validator;

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

import org.apache.avro.Schema;
import org.apache.avro.Schema.Type;
import org.radarcns.validator.config.ExcludeConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.radarcns.validator.SchemaValidationRoles.getActiveValidator;
import static org.radarcns.validator.SchemaValidationRoles.getGeneralEnumValidator;
import static org.radarcns.validator.SchemaValidationRoles.getGeneralRecordValidator;
import static org.radarcns.validator.SchemaValidationRoles.getMonitorValidator;
import static org.radarcns.validator.SchemaValidationRoles.getPassiveValidator;

final class SchemaValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(SchemaValidator.class);

    private static final Map<String, Schema> CACHE = new HashMap<>();

    private SchemaValidator() {
        //Static class
    }

    public static ValidationResult validate(Schema schema, Path pathToSchema, Scope root) {
        return validate(schema, pathToSchema, root,
                false, null);
    }

    public static ValidationResult validate(Schema schema, Path pathToSchema, Scope root,
            boolean skipRecordName, Set<String> skipFieldName) {
        Objects.requireNonNull(schema);
        Objects.requireNonNull(pathToSchema);
        Objects.requireNonNull(root);

        CACHE.put(schema.getFullName(), schema);

        ValidationResult result;

        if (schema.getType().equals(Type.ENUM)) {
            result = getGeneralEnumValidator(pathToSchema, root,
                    skipRecordName).apply(schema);
        } else {
            switch (root) {
                case ACTIVE:
                    result = getActiveValidator(pathToSchema, root, skipRecordName,
                        skipFieldName).apply(schema);
                    break;
                case CATALOGUE:
                    result = getGeneralRecordValidator(pathToSchema, root,
                        skipRecordName, skipFieldName).apply(schema);
                    break;
                case KAFKA:
                    result = getGeneralRecordValidator(pathToSchema, root,
                            skipRecordName, skipFieldName).apply(schema);
                    break;
                case MONITOR:
                    result = getMonitorValidator(pathToSchema, root, skipRecordName,
                        skipFieldName).apply(schema);
                    break;
                case PASSIVE:
                    result = getPassiveValidator(pathToSchema, root, skipRecordName,
                        skipFieldName).apply(schema);
                    break;
                default:
                    LOGGER.warn("Applying general validation to {}", getPath(pathToSchema));
                    result = getGeneralRecordValidator(pathToSchema, root, skipRecordName,
                            skipFieldName).apply(schema);
                    break;
            }
        }

        return result;
    }

    /**
     * TODO.
     * @param path TODO
     * @return TODO
     */
    public static String getPath(Path path) {
        return path.toString().substring(path.toString().indexOf(ExcludeConfig.REPOSITORY_NAME));
    }
}