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
package com.datastax.oss.sink.pulsar;

import com.datastax.oss.sink.pulsar.gen.GenSchema;
import com.datastax.oss.sink.pulsar.gen.GenStruct;
import org.apache.pulsar.client.api.schema.GenericRecord;
import org.apache.pulsar.functions.api.Record;
import org.apache.pulsar.io.core.annotations.Connector;
import org.apache.pulsar.io.core.annotations.IOType;

@Connector(
  name = "dssc-generic-gen",
  type = IOType.SINK,
  help = "PulsarSinkConnector is used for moving messages from Pulsar to Cassandra",
  configClass = PulsarSinkConfig.class
)
public class GenSchemaGenericRecordSink extends GenericRecordSink<GenStruct> {

  @Override
  protected APIAdapter<GenericRecord, GenStruct, ?, ?, ?, Header> createAPIAdapter() {
    return new GenAPIAdapter();
  }

  @Override
  protected GenStruct readValue(Record<GenericRecord> record) throws Exception {
    return GenSchema.convert(record.getValue());
  }
}