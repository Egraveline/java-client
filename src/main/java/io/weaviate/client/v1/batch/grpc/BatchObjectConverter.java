package io.weaviate.client.v1.batch.grpc;

import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import io.weaviate.client.v1.data.model.WeaviateObject;
import io.weaviate.client.grpc.protocol.v1.WeaviateProtoBase;
import io.weaviate.client.grpc.protocol.v1.WeaviateProtoBatch;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

public class BatchObjectConverter {

  public static WeaviateProtoBatch.BatchObject toBatchObject(WeaviateObject obj) {
    WeaviateProtoBatch.BatchObject.Builder builder = WeaviateProtoBatch.BatchObject.newBuilder();
    if (obj.getId() != null) {
      builder.setUuid(obj.getId());
    }
    if (obj.getClassName() != null) {
      builder.setCollection(obj.getClassName());
    }
    if (obj.getVector() != null) {
      builder.addAllVector(Arrays.asList(obj.getVector()));
    }
    if (obj.getTenant() != null) {
      builder.setTenant(obj.getTenant());
    }
    if (obj.getProperties() != null) {
      builder.setProperties(buildProperties(obj.getProperties()));
    }
    return builder.build();
  }

  @AllArgsConstructor
  @ToString
  @FieldDefaults(level = AccessLevel.PRIVATE)
  private static class Properties {
    Map<String, Value> nonRefProperties;
    List<WeaviateProtoBase.NumberArrayProperties> numberArrayProperties;
    List<WeaviateProtoBase.IntArrayProperties> intArrayProperties;
    List<WeaviateProtoBase.TextArrayProperties> textArrayProperties;
    List<WeaviateProtoBase.BooleanArrayProperties> booleanArrayProperties;
    List<WeaviateProtoBase.ObjectProperties> objectProperties;
    List<WeaviateProtoBase.ObjectArrayProperties> objectArrayProperties;
  }

  private static WeaviateProtoBatch.BatchObject.Properties buildProperties(Map<String, Object> properties) {
    WeaviateProtoBatch.BatchObject.Properties.Builder builder = WeaviateProtoBatch.BatchObject.Properties.newBuilder();

    Properties props = extractProperties(properties);
    builder.setNonRefProperties(Struct.newBuilder().putAllFields(props.nonRefProperties).build());
    props.numberArrayProperties.forEach(builder::addNumberArrayProperties);
    props.intArrayProperties.forEach(builder::addIntArrayProperties);
    props.textArrayProperties.forEach(builder::addTextArrayProperties);
    props.booleanArrayProperties.forEach(builder::addBooleanArrayProperties);
    props.objectProperties.forEach(builder::addObjectProperties);
    props.objectArrayProperties.forEach(builder::addObjectArrayProperties);

    return builder.build();
  }

  private static Properties extractProperties(Map<String, Object> properties) {
    Map<String, Value> nonRefProperties = new HashMap<>();
    List<WeaviateProtoBase.NumberArrayProperties> numberArrayProperties = new ArrayList<>();
    List<WeaviateProtoBase.IntArrayProperties> intArrayProperties = new ArrayList<>();
    List<WeaviateProtoBase.TextArrayProperties> textArrayProperties = new ArrayList<>();
    List<WeaviateProtoBase.BooleanArrayProperties> booleanArrayProperties = new ArrayList<>();
    List<WeaviateProtoBase.ObjectProperties> objectProperties = new ArrayList<>();
    List<WeaviateProtoBase.ObjectArrayProperties> objectArrayProperties = new ArrayList<>();
    // extract properties
    for (Map.Entry<String, Object> e : properties.entrySet()) {
      String propName = e.getKey();
      Object propValue = e.getValue();
      if (propName.equals("_lastUpdateTimeUnix") || propName.equals("_creationTimeUnix")) {
        // ignore for now _creationTimeUnix / _lastUpdateTimeUnix
        continue;
      }
      if (propValue instanceof String) {
        nonRefProperties.put(propName, Value.newBuilder().setStringValue((String) propValue).build());
        continue;
      }
      if (propValue instanceof Boolean) {
        nonRefProperties.put(propName, Value.newBuilder().setBoolValue((Boolean) propValue).build());
        continue;
      }
      if (propValue instanceof Integer) {
        nonRefProperties.put(propName, Value.newBuilder().setNumberValue(((Integer) propValue).doubleValue()).build());
        continue;
      }
      if (propValue instanceof Long) {
        nonRefProperties.put(propName, Value.newBuilder().setNumberValue(((Long) propValue).doubleValue()).build());
        continue;
      }
      if (propValue instanceof Float) {
        nonRefProperties.put(propName, Value.newBuilder().setNumberValue(((Float) propValue).doubleValue()).build());
        continue;
      }
      if (propValue instanceof Double) {
        nonRefProperties.put(propName, Value.newBuilder().setNumberValue((Double) propValue).build());
        continue;
      }
      if (propValue instanceof String[]) {
        // TODO: handle ref properties
        WeaviateProtoBase.TextArrayProperties textArrayProps = WeaviateProtoBase.TextArrayProperties.newBuilder()
          .setPropName(propName).addAllValues(Arrays.asList((String[]) propValue)).build();
        textArrayProperties.add(textArrayProps);
        continue;
      }
      if (propValue instanceof Boolean[]) {
        WeaviateProtoBase.BooleanArrayProperties booleanArrayProps = WeaviateProtoBase.BooleanArrayProperties.newBuilder()
          .setPropName(propName).addAllValues(Arrays.asList((Boolean[]) propValue)).build();
        booleanArrayProperties.add(booleanArrayProps);
        continue;
      }
      if (propValue instanceof Integer[]) {
        List<Long> value = Arrays.stream((Integer[]) propValue).map(Integer::longValue).collect(Collectors.toList());
        WeaviateProtoBase.IntArrayProperties intArrayProps = WeaviateProtoBase.IntArrayProperties.newBuilder()
          .setPropName(propName).addAllValues(value).build();
        intArrayProperties.add(intArrayProps);
        continue;
      }
      if (propValue instanceof Long[]) {
        WeaviateProtoBase.IntArrayProperties intArrayProps = WeaviateProtoBase.IntArrayProperties.newBuilder()
          .setPropName(propName)
          .addAllValues(Arrays.asList((Long[]) propValue))
          .build();
        intArrayProperties.add(intArrayProps);
        continue;
      }
      if (propValue instanceof Float[]) {
        List<Double> value = Arrays.stream((Float[]) propValue).map(Float::doubleValue).collect(Collectors.toList());
        WeaviateProtoBase.NumberArrayProperties numberArrayProps = WeaviateProtoBase.NumberArrayProperties.newBuilder()
          .setPropName(propName).addAllValues(value).build();
        numberArrayProperties.add(numberArrayProps);
        continue;
      }
      if (propValue instanceof Double[]) {
        WeaviateProtoBase.NumberArrayProperties numberArrayProps = WeaviateProtoBase.NumberArrayProperties.newBuilder()
          .setPropName(propName).addAllValues(Arrays.asList((Double[]) propValue)).build();
        numberArrayProperties.add(numberArrayProps);
        continue;
      }
      if (propValue instanceof Map) {
        Properties extractedProperties = extractProperties((Map<String, Object>) propValue);
        WeaviateProtoBase.ObjectPropertiesValue.Builder objectPropertiesValue = WeaviateProtoBase.ObjectPropertiesValue.newBuilder();
        objectPropertiesValue.setNonRefProperties(Struct.newBuilder().putAllFields(extractedProperties.nonRefProperties).build());
        extractedProperties.numberArrayProperties.forEach(objectPropertiesValue::addNumberArrayProperties);
        extractedProperties.intArrayProperties.forEach(objectPropertiesValue::addIntArrayProperties);
        extractedProperties.textArrayProperties.forEach(objectPropertiesValue::addTextArrayProperties);
        extractedProperties.booleanArrayProperties.forEach(objectPropertiesValue::addBooleanArrayProperties);
        extractedProperties.objectProperties.forEach(objectPropertiesValue::addObjectProperties);
        extractedProperties.objectArrayProperties.forEach(objectPropertiesValue::addObjectArrayProperties);

        WeaviateProtoBase.ObjectProperties objectProps = WeaviateProtoBase.ObjectProperties.newBuilder()
          .setPropName(propName).setValue(objectPropertiesValue.build()).build();

        objectProperties.add(objectProps);
        continue;
      }
      if (propValue instanceof List) {
        List<WeaviateProtoBase.ObjectPropertiesValue> objectPropertiesValues = new ArrayList<>();
        for (Object propValueObject : (List) propValue) {
          if (propValueObject instanceof Map) {
            Properties extractedProperties = extractProperties((Map<String, Object>) propValueObject);
            WeaviateProtoBase.ObjectPropertiesValue.Builder objectPropertiesValue = WeaviateProtoBase.ObjectPropertiesValue.newBuilder();
            objectPropertiesValue.setNonRefProperties(Struct.newBuilder().putAllFields(extractedProperties.nonRefProperties).build());
            extractedProperties.numberArrayProperties.forEach(objectPropertiesValue::addNumberArrayProperties);
            extractedProperties.intArrayProperties.forEach(objectPropertiesValue::addIntArrayProperties);
            extractedProperties.textArrayProperties.forEach(objectPropertiesValue::addTextArrayProperties);
            extractedProperties.booleanArrayProperties.forEach(objectPropertiesValue::addBooleanArrayProperties);
            extractedProperties.objectProperties.forEach(objectPropertiesValue::addObjectProperties);
            extractedProperties.objectArrayProperties.forEach(objectPropertiesValue::addObjectArrayProperties);

            objectPropertiesValues.add(objectPropertiesValue.build());
          }
        }

        WeaviateProtoBase.ObjectArrayProperties objectArrayProps = WeaviateProtoBase.ObjectArrayProperties.newBuilder()
          .setPropName(propName).addAllValues(objectPropertiesValues).build();

        objectArrayProperties.add(objectArrayProps);
      }
    }
    return new Properties(nonRefProperties, numberArrayProperties, intArrayProperties, textArrayProperties,
      booleanArrayProperties, objectProperties, objectArrayProperties);
  }
}
