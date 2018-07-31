/*
 * Copyright DataStax, Inc.
 *
 * This software is subject to the below license agreement.
 * DataStax may make changes to the agreement from time to time,
 * and will post the amended terms at
 * https://www.datastax.com/terms/datastax-dse-bulk-utility-license-terms.
 */
package com.datastax.kafkaconnector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.datastax.oss.driver.api.core.type.DataType;
import com.datastax.oss.driver.api.core.type.DataTypes;
import com.datastax.oss.driver.api.core.type.reflect.GenericType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

class KeyValueRecordMetadataTest {

  private RecordMetadata keyMetadata;
  private RecordMetadata valueMetadata;

  @BeforeEach
  void setUp() {
    keyMetadata = mock(RecordMetadata.class);
    when((GenericType) keyMetadata.getFieldType(ArgumentMatchers.eq("kf1"), any(DataType.class)))
        .thenReturn(GenericType.STRING);

    valueMetadata = mock(RecordMetadata.class);
    when((GenericType) valueMetadata.getFieldType(ArgumentMatchers.eq("vf1"), any(DataType.class)))
        .thenReturn(GenericType.BIG_INTEGER);
  }

  @Test
  void should_qualify_field_names() {
    KeyValueRecordMetadata metadata = new KeyValueRecordMetadata(keyMetadata, valueMetadata);
    assertThat(metadata.getFieldType("key.kf1", DataTypes.TEXT)).isEqualTo(GenericType.STRING);
    assertThat(metadata.getFieldType("value.vf1", DataTypes.BIGINT))
        .isEqualTo(GenericType.BIG_INTEGER);
  }

  @Test
  void should_qualify_field_names_keys_only() {
    KeyValueRecordMetadata metadata = new KeyValueRecordMetadata(keyMetadata, null);
    assertThat(metadata.getFieldType("key.kf1", DataTypes.TEXT)).isEqualTo(GenericType.STRING);
    assertThat(metadata.getFieldType("value.vf1", DataTypes.BIGINT)).isNull();
  }

  @Test
  void should_qualify_field_names_values_only() {
    KeyValueRecordMetadata metadata = new KeyValueRecordMetadata(null, valueMetadata);
    assertThat(metadata.getFieldType("value.vf1", DataTypes.BIGINT))
        .isEqualTo(GenericType.BIG_INTEGER);
    assertThat(metadata.getFieldType("key.kf1", DataTypes.TEXT)).isNull();
  }
}