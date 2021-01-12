/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.amplifyframework.datastore.appsync;

import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.core.model.ModelSchemaRegistry;
import com.amplifyframework.util.GsonFactory;
import com.amplifyframework.util.GsonObjectConverter;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility for converting a Model to/from a Map<String, Object> or a SerializedModel.
 */
public class ModelConverter {
    public static <T extends Model> Map<String, Object> toMap(T model) {
        if(model == null) {
            return new HashMap<>();
        }
        if(model instanceof SerializedModel) {
            return ((SerializedModel) model).getSerializedData();
        } else {
            Gson gson = GsonFactory.instance();
            JsonElement jsonElement = gson.toJsonTree(model);
            return GsonObjectConverter.toMap(jsonElement.getAsJsonObject());
        }
    }

    public static <T extends Model> T fromMap(Map<String, Object> map, Class<T> modelClass) {
        Gson gson = GsonFactory.instance();
        String jsonString = gson.toJson(map);
        return gson.fromJson(jsonString, modelClass);
    }
}

