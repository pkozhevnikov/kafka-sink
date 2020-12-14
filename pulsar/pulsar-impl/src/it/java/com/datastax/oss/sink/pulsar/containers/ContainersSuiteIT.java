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
package com.datastax.oss.sink.pulsar.containers;

import com.datastax.oss.sink.pulsar.GenSchemaGenericRecordSink;
import com.datastax.oss.sink.pulsar.GenericRecordSink;
import com.datastax.oss.sink.pulsar.ReflectionGenericRecordSink;
import com.datastax.oss.sink.pulsar.SchemedGenericRecordSink;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;

@Tag("containers")
class ContainersSuiteIT {

  @Nested
  class Bytes extends BytesSinkPart {}

  @Nested
  class Schemed extends GenericRecordSinkPart {

    @Override
    protected String basicName() {
      return "schemed-gr";
    }

    @Override
    protected Class<? extends GenericRecordSink> sinkClass() {
      return SchemedGenericRecordSink.class;
    }
  }

  @Nested
  class Reflected extends GenericRecordSinkPart {

    @Override
    protected String basicName() {
      return "reflected-gr";
    }

    @Override
    protected Class<? extends GenericRecordSink> sinkClass() {
      return ReflectionGenericRecordSink.class;
    }
  }

  @Nested
  class GenSchema extends GenericRecordSinkPart {

    @Override
    protected String basicName() {
      return "genschema-gr";
    }

    @Override
    protected Class<? extends GenericRecordSink> sinkClass() {
      return GenSchemaGenericRecordSink.class;
    }
  }
}
