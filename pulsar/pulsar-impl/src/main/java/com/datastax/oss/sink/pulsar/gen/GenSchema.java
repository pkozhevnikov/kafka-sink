/*
 * Copyright DataStax, Inc.
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
package com.datastax.oss.sink.pulsar.gen;

import com.datastax.oss.driver.shaded.guava.common.collect.ImmutableMap;
import com.datastax.oss.sink.record.SchemaSupport;
import com.datastax.oss.sink.util.Tuple2;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.apache.avro.util.Utf8;
import org.apache.pulsar.client.api.schema.GenericRecord;

public class GenSchema {

  public final SchemaSupport.Type type;

  private GenSchema(SchemaSupport.Type type) {
    this.type = type;
  }

  public static GenStruct convert(GenericRecord record) {
    return (GenStruct) adjustValue(record);
  }

  public static GenStruct convert(JsonNode node) {
    return (GenStruct) adjustJsonValue(node);
  }

  private static Object adjustValue(Object o) {
    if (o == null || "null".equals(o)) return null;
    if (o instanceof byte[]) return ByteBuffer.wrap((byte[]) o);
    else if (o instanceof Utf8) return o.toString();
    else if (o instanceof Map)
      return ((Map<?, ?>) o)
          .entrySet()
          .stream()
          .map(et -> Tuple2.of(et.getKey(), adjustValue(et.getValue())))
          .collect(HashMap::new, (m, t) -> m.put(t._1, t._2), HashMap::putAll);
    else if (o instanceof List)
      return ((List<?>) o).stream().map(GenSchema::adjustValue).collect(Collectors.toList());
    else if (o instanceof GenericRecord) {
      GenericRecord record = (GenericRecord) o;
      StructGenSchema schema = (StructGenSchema) inferSchema(record);
      Map<String, ?> values =
          schema
              .fieldNames()
              .stream()
              .map(f -> Tuple2.of(f, adjustValue(record.getField(f))))
              .collect(HashMap::new, (m, t) -> m.put(t._1, t._2), HashMap::putAll);
      return new GenStruct(values, schema);
    } else return o;
  }

  private static Object adjustJsonValue(JsonNode o) {
    if (o == null || o.isNull()) return null;
    if (o.isBinary()) {
      try {
        return ByteBuffer.wrap(o.binaryValue());
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    } else if (o.isTextual()) return o.asText();
    else if (o.isShort()) return o.shortValue();
    else if (o.isInt()) return o.intValue();
    else if (o.isFloat()) return o.floatValue();
    else if (o.isDouble()) return o.doubleValue();
    else if (o.isBoolean()) return o.booleanValue();
    else if (o.isArray())
      return StreamSupport.stream(o.spliterator(), false)
          .map(GenSchema::adjustJsonValue)
          .collect(Collectors.toList());
    else if (o.isObject()) {
      StructGenSchema schema = (StructGenSchema) inferSchema(o);
      Map<String, ?> values =
          schema
              .fieldNames()
              .stream()
              .map(f -> Tuple2.of(f, adjustJsonValue(o.get(f))))
              .collect(HashMap::new, (m, t) -> m.put(t._1, t._2), HashMap::putAll);
      return new GenStruct(values, schema);
    } else return o;
  }

  private static GenSchema inferSchema(Object value) {
    if (value == null) return STRING;
    if (value instanceof GenericRecord) {
      GenericRecord record = (GenericRecord) value;
      StructGenSchema schema = new StructGenSchema(record.getFields().size());
      record
          .getFields()
          .forEach(field -> schema.addField(field.getName(), inferSchema(record.getField(field))));
      return schema;
    } else if (value instanceof Map) {
      @SuppressWarnings("unchecked")
      Map<String, ?> map = (Map<String, ?>) value;
      if (map.isEmpty()) return new MapGenSchema(GenSchema.STRING);
      return new MapGenSchema(inferSchema(map.values().iterator().next()));
    } else if (value instanceof List) {
      List<?> list = (List<?>) value;
      if (list.isEmpty()) return new ArrayGenSchema(GenSchema.STRING);
      return new ArrayGenSchema(inferSchema(list.get(0)));
    } else if (value instanceof String) {
      return STRING;
    } else if (value instanceof Utf8) {
      return STRING;
    } else if (value instanceof Byte) {
      return INT8;
    } else if (value instanceof Short) {
      return INT16;
    } else if (value instanceof Integer) {
      return INT32;
    } else if (value instanceof Long) {
      return INT64;
    } else if (value instanceof Float) {
      return FLOAT32;
    } else if (value instanceof Double) {
      return FLOAT64;
    } else if (value instanceof Boolean) {
      return BOOLEAN;
    } else if (value instanceof byte[]) {
      return BYTES;
    } else if (value instanceof ByteBuffer) {
      return BYTES;
    }
    throw new IllegalArgumentException(
        String.format("could not infer schema of (%s) %s", value.getClass().getName(), value));
  }

  private static GenSchema inferSchema(JsonNode value) {
    if (value == null || value.isNull()) return STRING;
    if (value.isObject()) {
      StructGenSchema schema = new StructGenSchema(value.size());
      for (Iterator<Map.Entry<String, JsonNode>> it = value.fields(); it.hasNext(); ) {
        Map.Entry<String, JsonNode> field = it.next();
        schema.addField(field.getKey(), inferSchema(field.getValue()));
      }
      return schema;
    } else if (value.isArray()) {
      if (value.isEmpty()) return new ArrayGenSchema(GenSchema.STRING);
      return new ArrayGenSchema(inferSchema(value.elements().next()));
    } else if (value.isTextual()) {
      return STRING;
    } else if (value.isShort()) {
      return INT16;
    } else if (value.isInt()) {
      return INT32;
    } else if (value.isBigInteger()) {
      return INT64;
    } else if (value.isFloat()) {
      return FLOAT32;
    } else if (value.isDouble()) {
      return FLOAT64;
    } else if (value.isBoolean()) {
      return BOOLEAN;
    } else if (value.isBinary()) {
      return BYTES;
    }
    throw new IllegalArgumentException(
        String.format("could not infer schema of (%s) %s", value.getClass().getName(), value));
  }

  public static class StructGenSchema extends GenSchema {
    private StructGenSchema(int fieldNum) {
      super(SchemaSupport.Type.STRUCT);
      fields = new HashMap<>(fieldNum);
    }

    private final Map<String, GenSchema> fields;

    private void addField(String name, GenSchema schema) {
      fields.put(name, schema);
    }

    public GenSchema field(String fieldName) {
      return fields.get(fieldName);
    }

    public Set<String> fieldNames() {
      return fields.keySet();
    }
  }

  public abstract static class CollectionGenSchema extends GenSchema {
    private final GenSchema elementSchema;

    private CollectionGenSchema(SchemaSupport.Type type, GenSchema elementSchema) {
      super(type);
      this.elementSchema = elementSchema;
    }

    public GenSchema elementSchema() {
      return elementSchema;
    }
  }

  public static class ArrayGenSchema extends CollectionGenSchema {
    private ArrayGenSchema(GenSchema elementSchema) {
      super(SchemaSupport.Type.ARRAY, elementSchema);
    }
  }

  public static class MapGenSchema extends CollectionGenSchema {
    private MapGenSchema(GenSchema valueSchema) {
      super(SchemaSupport.Type.MAP, valueSchema);
    }

    public static final GenSchema KEY_SCHEMA = STRING;
  }

  public static final GenSchema BOOLEAN = new GenSchema(SchemaSupport.Type.BOOLEAN);
  public static final GenSchema BYTES = new GenSchema(SchemaSupport.Type.BYTES);
  public static final GenSchema FLOAT32 = new GenSchema(SchemaSupport.Type.FLOAT32);
  public static final GenSchema FLOAT64 = new GenSchema(SchemaSupport.Type.FLOAT64);
  public static final GenSchema INT32 = new GenSchema(SchemaSupport.Type.INT32);
  public static final GenSchema INT64 = new GenSchema(SchemaSupport.Type.INT64);
  public static final GenSchema INT8 = new GenSchema(SchemaSupport.Type.INT8);
  public static final GenSchema INT16 = new GenSchema(SchemaSupport.Type.INT16);
  public static final GenSchema STRING = new GenSchema(SchemaSupport.Type.STRING);
  public static final GenSchema NULL = new GenSchema(SchemaSupport.Type.NULL);

  private static final Map<Class<?>, GenSchema> primitiveSchemas =
      ImmutableMap.<Class<?>, GenSchema>builder()
          .put(Boolean.class, BOOLEAN)
          .put(boolean.class, BOOLEAN)
          .put(String.class, STRING)
          .put(Utf8.class, STRING)
          .put(Double.class, FLOAT64)
          .put(double.class, FLOAT64)
          .put(Float.class, FLOAT32)
          .put(float.class, FLOAT32)
          .put(Integer.class, INT32)
          .put(int.class, INT32)
          .put(Long.class, INT64)
          .put(long.class, INT64)
          .put(Short.class, INT16)
          .put(short.class, INT16)
          .put(Byte.class, INT8)
          .put(byte.class, INT8)
          .put(byte[].class, BYTES)
          .put(ByteBuffer.class, BYTES)
          .build();

  public static GenSchema schemaOf(Object o) {
    if (o == null) return NULL;
    if (o instanceof ByteBuffer) return BYTES;
    GenSchema schema = primitiveSchemas.get(o.getClass());
    if (schema == null) schema = ((GenStruct) o).getSchema();
    return schema;
  }
}
