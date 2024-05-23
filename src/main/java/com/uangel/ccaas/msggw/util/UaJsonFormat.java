package com.uangel.ccaas.msggw.util;

import com.google.common.base.Preconditions;
import com.google.common.io.BaseEncoding;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.protobuf.*;
import com.google.protobuf.util.Durations;
import com.google.protobuf.util.FieldMaskUtil;
import com.google.protobuf.util.Timestamps;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.ParseException;
import java.util.*;

/**
 * @author kangmoo Heo
 */


/**
 * 기존 JsonFormat을 수정한 UaJsonFormat
 * bytes 출력시 로그 레벨에 따라 다음과 같이 동작
 *   - TRACE 레벨 : bytes 를 문자열 변환하여 출력 (기존 JsonFormat과 동일)
 *   - DEBUG 레벨 : bytes 를 문자열 변환 후, `BYTES_LIMIT` 만큼의 길이만 출력
 *   - INFO 레벨 이상 : bytes 길이만 출력
 */

@Slf4j
public class UaJsonFormat {
    // Debug 레벨에서 출력할 bytes 길이
    private static final int BYTES_LIMIT = 128;

    private UaJsonFormat() {
    }

    public static Printer printer() {
        return new Printer(com.google.protobuf.TypeRegistry.getEmptyTypeRegistry(), TypeRegistry.getEmptyTypeRegistry(), false, Collections.emptySet(), false, false, false, false);
    }

    public static Parser parser() {
        return new Parser(com.google.protobuf.TypeRegistry.getEmptyTypeRegistry(), TypeRegistry.getEmptyTypeRegistry(), false, 100);
    }

    private static String unsignedToString(final int value) {
        return value >= 0 ? Integer.toString(value) : Long.toString((long) value & 4294967295L);
    }

    private static String unsignedToString(final long value) {
        return value >= 0L ? Long.toString(value) : BigInteger.valueOf(value & Long.MAX_VALUE).setBit(63).toString();
    }

    private static String getTypeName(String typeUrl) throws InvalidProtocolBufferException {
        String[] parts = typeUrl.split("/");
        if (parts.length == 1) {
            throw new InvalidProtocolBufferException("Invalid type url found: " + typeUrl);
        } else {
            return parts[parts.length - 1];
        }
    }

    private static class ParserImpl {
        private final com.google.protobuf.TypeRegistry registry;
        private final TypeRegistry oldRegistry;
        private final boolean ignoringUnknownFields;
        private final int recursionLimit;
        private int currentDepth;
        private static final Map<String, WellKnownTypeParser> wellKnownTypeParsers = buildWellKnownTypeParsers();
        private final Map<Descriptors.Descriptor, Map<String, Descriptors.FieldDescriptor>> fieldNameMaps = new HashMap();
        private static final BigInteger MAX_UINT64 = new BigInteger("FFFFFFFFFFFFFFFF", 16);
        private static final double EPSILON = 1.0E-6;
        private static final BigDecimal MORE_THAN_ONE = new BigDecimal(String.valueOf(1.000001));
        private static final BigDecimal MAX_DOUBLE;
        private static final BigDecimal MIN_DOUBLE;

        ParserImpl(com.google.protobuf.TypeRegistry registry, TypeRegistry oldRegistry, boolean ignoreUnknownFields, int recursionLimit) {
            this.registry = registry;
            this.oldRegistry = oldRegistry;
            this.ignoringUnknownFields = ignoreUnknownFields;
            this.recursionLimit = recursionLimit;
            this.currentDepth = 0;
        }

        void merge(Reader json, Message.Builder builder) throws IOException {
            try {
                JsonReader reader = new JsonReader(json);
                reader.setLenient(false);
                this.merge(JsonParser.parseReader(reader), builder);
            } catch (JsonIOException var4) {
                if (var4.getCause() instanceof IOException) {
                    throw (IOException) var4.getCause();
                } else {
                    throw new InvalidProtocolBufferException(var4.getMessage(), var4);
                }
            } catch (RuntimeException var5) {
                throw new InvalidProtocolBufferException(var5.getMessage(), var5);
            }
        }

        void merge(String json, Message.Builder builder) throws InvalidProtocolBufferException {
            try {
                JsonReader reader = new JsonReader(new StringReader(json));
                reader.setLenient(false);
                this.merge(JsonParser.parseReader(reader), builder);
            } catch (RuntimeException var5) {
                InvalidProtocolBufferException toThrow = new InvalidProtocolBufferException(var5.getMessage());
                toThrow.initCause(var5);
                throw toThrow;
            }
        }

        private static Map<String, WellKnownTypeParser> buildWellKnownTypeParsers() {
            Map<String, WellKnownTypeParser> parsers = new HashMap();
            parsers.put(Any.getDescriptor().getFullName(), new WellKnownTypeParser() {
                public void merge(ParserImpl parser, JsonElement json, Message.Builder builder) throws InvalidProtocolBufferException {
                    parser.mergeAny(json, builder);
                }
            });
            WellKnownTypeParser wrappersPrinter = new WellKnownTypeParser() {
                public void merge(ParserImpl parser, JsonElement json, Message.Builder builder) throws InvalidProtocolBufferException {
                    parser.mergeWrapper(json, builder);
                }
            };
            parsers.put(BoolValue.getDescriptor().getFullName(), wrappersPrinter);
            parsers.put(Int32Value.getDescriptor().getFullName(), wrappersPrinter);
            parsers.put(UInt32Value.getDescriptor().getFullName(), wrappersPrinter);
            parsers.put(Int64Value.getDescriptor().getFullName(), wrappersPrinter);
            parsers.put(UInt64Value.getDescriptor().getFullName(), wrappersPrinter);
            parsers.put(StringValue.getDescriptor().getFullName(), wrappersPrinter);
            parsers.put(BytesValue.getDescriptor().getFullName(), wrappersPrinter);
            parsers.put(FloatValue.getDescriptor().getFullName(), wrappersPrinter);
            parsers.put(DoubleValue.getDescriptor().getFullName(), wrappersPrinter);
            parsers.put(Timestamp.getDescriptor().getFullName(), new WellKnownTypeParser() {
                public void merge(ParserImpl parser, JsonElement json, Message.Builder builder) throws InvalidProtocolBufferException {
                    parser.mergeTimestamp(json, builder);
                }
            });
            parsers.put(Duration.getDescriptor().getFullName(), new WellKnownTypeParser() {
                public void merge(ParserImpl parser, JsonElement json, Message.Builder builder) throws InvalidProtocolBufferException {
                    parser.mergeDuration(json, builder);
                }
            });
            parsers.put(FieldMask.getDescriptor().getFullName(), new WellKnownTypeParser() {
                public void merge(ParserImpl parser, JsonElement json, Message.Builder builder) throws InvalidProtocolBufferException {
                    parser.mergeFieldMask(json, builder);
                }
            });
            parsers.put(Struct.getDescriptor().getFullName(), new WellKnownTypeParser() {
                public void merge(ParserImpl parser, JsonElement json, Message.Builder builder) throws InvalidProtocolBufferException {
                    parser.mergeStruct(json, builder);
                }
            });
            parsers.put(ListValue.getDescriptor().getFullName(), new WellKnownTypeParser() {
                public void merge(ParserImpl parser, JsonElement json, Message.Builder builder) throws InvalidProtocolBufferException {
                    parser.mergeListValue(json, builder);
                }
            });
            parsers.put(Value.getDescriptor().getFullName(), new WellKnownTypeParser() {
                public void merge(ParserImpl parser, JsonElement json, Message.Builder builder) throws InvalidProtocolBufferException {
                    parser.mergeValue(json, builder);
                }
            });
            return parsers;
        }

        private void merge(JsonElement json, Message.Builder builder) throws InvalidProtocolBufferException {
            WellKnownTypeParser specialParser = (WellKnownTypeParser) wellKnownTypeParsers.get(builder.getDescriptorForType().getFullName());
            if (specialParser != null) {
                specialParser.merge(this, json, builder);
            } else {
                this.mergeMessage(json, builder, false);
            }
        }

        private Map<String, Descriptors.FieldDescriptor> getFieldNameMap(Descriptors.Descriptor descriptor) {
            if (this.fieldNameMaps.containsKey(descriptor)) {
                return (Map) this.fieldNameMaps.get(descriptor);
            } else {
                Map<String, Descriptors.FieldDescriptor> fieldNameMap = new HashMap();
                Iterator var3 = descriptor.getFields().iterator();

                while (var3.hasNext()) {
                    Descriptors.FieldDescriptor field = (Descriptors.FieldDescriptor) var3.next();
                    fieldNameMap.put(field.getName(), field);
                    fieldNameMap.put(field.getJsonName(), field);
                }

                this.fieldNameMaps.put(descriptor, fieldNameMap);
                return fieldNameMap;
            }
        }

        private void mergeMessage(JsonElement json, Message.Builder builder, boolean skipTypeUrl) throws InvalidProtocolBufferException {
            if (!(json instanceof JsonObject)) {
                throw new InvalidProtocolBufferException("Expect message object but got: " + json);
            } else {
                JsonObject object = (JsonObject) json;
                Map<String, Descriptors.FieldDescriptor> fieldNameMap = this.getFieldNameMap(builder.getDescriptorForType());
                Iterator var6 = object.entrySet().iterator();

                while (true) {
                    Map.Entry entry;
                    do {
                        if (!var6.hasNext()) {
                            return;
                        }

                        entry = (Map.Entry) var6.next();
                    } while (skipTypeUrl && ((String) entry.getKey()).equals("@type"));

                    Descriptors.FieldDescriptor field = (Descriptors.FieldDescriptor) fieldNameMap.get(entry.getKey());
                    if (field == null) {
                        if (!this.ignoringUnknownFields) {
                            throw new InvalidProtocolBufferException("Cannot find field: " + (String) entry.getKey() + " in message " + builder.getDescriptorForType().getFullName());
                        }
                    } else {
                        this.mergeField(field, (JsonElement) entry.getValue(), builder);
                    }
                }
            }
        }

        private void mergeAny(JsonElement json, Message.Builder builder) throws InvalidProtocolBufferException {
            Descriptors.Descriptor descriptor = builder.getDescriptorForType();
            Descriptors.FieldDescriptor typeUrlField = descriptor.findFieldByName("type_url");
            Descriptors.FieldDescriptor valueField = descriptor.findFieldByName("value");
            if (typeUrlField != null && valueField != null && typeUrlField.getType() == Descriptors.FieldDescriptor.Type.STRING && valueField.getType() == Descriptors.FieldDescriptor.Type.BYTES) {
                if (!(json instanceof JsonObject)) {
                    throw new InvalidProtocolBufferException("Expect message object but got: " + json);
                } else {
                    JsonObject object = (JsonObject) json;
                    if (!object.entrySet().isEmpty()) {
                        JsonElement typeUrlElement = object.get("@type");
                        if (typeUrlElement == null) {
                            throw new InvalidProtocolBufferException("Missing type url when parsing: " + json);
                        } else {
                            String typeUrl = typeUrlElement.getAsString();
                            Descriptors.Descriptor contentType = this.registry.getDescriptorForTypeUrl(typeUrl);
                            if (contentType == null) {
                                contentType = this.oldRegistry.getDescriptorForTypeUrl(typeUrl);
                                if (contentType == null) {
                                    throw new InvalidProtocolBufferException("Cannot resolve type: " + typeUrl);
                                }
                            }

                            builder.setField(typeUrlField, typeUrl);
                            Message.Builder contentBuilder = DynamicMessage.getDefaultInstance(contentType).newBuilderForType();
                            WellKnownTypeParser specialParser = (WellKnownTypeParser) wellKnownTypeParsers.get(contentType.getFullName());
                            if (specialParser != null) {
                                JsonElement value = object.get("value");
                                if (value != null) {
                                    specialParser.merge(this, value, contentBuilder);
                                }
                            } else {
                                this.mergeMessage(json, contentBuilder, true);
                            }

                            builder.setField(valueField, contentBuilder.build().toByteString());
                        }
                    }
                }
            } else {
                throw new InvalidProtocolBufferException("Invalid Any type.");
            }
        }

        private void mergeFieldMask(JsonElement json, Message.Builder builder) throws InvalidProtocolBufferException {
            FieldMask value = FieldMaskUtil.fromJsonString(json.getAsString());
            builder.mergeFrom(value.toByteString());
        }

        private void mergeTimestamp(JsonElement json, Message.Builder builder) throws InvalidProtocolBufferException {
            try {
                Timestamp value = Timestamps.parse(json.getAsString());
                builder.mergeFrom(value.toByteString());
            } catch (UnsupportedOperationException | ParseException var5) {
                InvalidProtocolBufferException ex = new InvalidProtocolBufferException("Failed to parse timestamp: " + json);
                ex.initCause(var5);
                throw ex;
            }
        }

        private void mergeDuration(JsonElement json, Message.Builder builder) throws InvalidProtocolBufferException {
            try {
                Duration value = Durations.parse(json.getAsString());
                builder.mergeFrom(value.toByteString());
            } catch (UnsupportedOperationException | ParseException var5) {
                InvalidProtocolBufferException ex = new InvalidProtocolBufferException("Failed to parse duration: " + json);
                ex.initCause(var5);
                throw ex;
            }
        }

        private void mergeStruct(JsonElement json, Message.Builder builder) throws InvalidProtocolBufferException {
            Descriptors.Descriptor descriptor = builder.getDescriptorForType();
            Descriptors.FieldDescriptor field = descriptor.findFieldByName("fields");
            if (field == null) {
                throw new InvalidProtocolBufferException("Invalid Struct type.");
            } else {
                this.mergeMapField(field, json, builder);
            }
        }

        private void mergeListValue(JsonElement json, Message.Builder builder) throws InvalidProtocolBufferException {
            Descriptors.Descriptor descriptor = builder.getDescriptorForType();
            Descriptors.FieldDescriptor field = descriptor.findFieldByName("values");
            if (field == null) {
                throw new InvalidProtocolBufferException("Invalid ListValue type.");
            } else {
                this.mergeRepeatedField(field, json, builder);
            }
        }

        private void mergeValue(JsonElement json, Message.Builder builder) throws InvalidProtocolBufferException {
            Descriptors.Descriptor type = builder.getDescriptorForType();
            if (json instanceof JsonPrimitive) {
                JsonPrimitive primitive = (JsonPrimitive) json;
                if (primitive.isBoolean()) {
                    builder.setField(type.findFieldByName("bool_value"), primitive.getAsBoolean());
                } else if (primitive.isNumber()) {
                    builder.setField(type.findFieldByName("number_value"), primitive.getAsDouble());
                } else {
                    builder.setField(type.findFieldByName("string_value"), primitive.getAsString());
                }
            } else {
                Message.Builder listBuilder;
                Descriptors.FieldDescriptor field;
                if (json instanceof JsonObject) {
                    field = type.findFieldByName("struct_value");
                    listBuilder = builder.newBuilderForField(field);
                    this.merge(json, listBuilder);
                    builder.setField(field, listBuilder.build());
                } else if (json instanceof JsonArray) {
                    field = type.findFieldByName("list_value");
                    listBuilder = builder.newBuilderForField(field);
                    this.merge(json, listBuilder);
                    builder.setField(field, listBuilder.build());
                } else {
                    if (!(json instanceof JsonNull)) {
                        throw new IllegalStateException("Unexpected json data: " + json);
                    }

                    builder.setField(type.findFieldByName("null_value"), NullValue.NULL_VALUE.getValueDescriptor());
                }
            }

        }

        private void mergeWrapper(JsonElement json, Message.Builder builder) throws InvalidProtocolBufferException {
            Descriptors.Descriptor type = builder.getDescriptorForType();
            Descriptors.FieldDescriptor field = type.findFieldByName("value");
            if (field == null) {
                throw new InvalidProtocolBufferException("Invalid wrapper type: " + type.getFullName());
            } else {
                builder.setField(field, this.parseFieldValue(field, json, builder));
            }
        }

        private void mergeField(Descriptors.FieldDescriptor field, JsonElement json, Message.Builder builder) throws InvalidProtocolBufferException {
            if (field.isRepeated()) {
                if (builder.getRepeatedFieldCount(field) > 0) {
                    throw new InvalidProtocolBufferException("Field " + field.getFullName() + " has already been set.");
                }
            } else if (builder.hasField(field)) {
                throw new InvalidProtocolBufferException("Field " + field.getFullName() + " has already been set.");
            }

            if (!field.isRepeated() || !(json instanceof JsonNull)) {
                if (field.isMapField()) {
                    this.mergeMapField(field, json, builder);
                } else if (field.isRepeated()) {
                    this.mergeRepeatedField(field, json, builder);
                } else if (field.getContainingOneof() != null) {
                    this.mergeOneofField(field, json, builder);
                } else {
                    Object value = this.parseFieldValue(field, json, builder);
                    if (value != null) {
                        builder.setField(field, value);
                    }
                }

            }
        }

        private void mergeMapField(Descriptors.FieldDescriptor field, JsonElement json, Message.Builder builder) throws InvalidProtocolBufferException {
            if (!(json instanceof JsonObject)) {
                throw new InvalidProtocolBufferException("Expect a map object but found: " + json);
            } else {
                Descriptors.Descriptor type = field.getMessageType();
                Descriptors.FieldDescriptor keyField = type.findFieldByName("key");
                Descriptors.FieldDescriptor valueField = type.findFieldByName("value");
                if (keyField != null && valueField != null) {
                    JsonObject object = (JsonObject) json;
                    Iterator var8 = object.entrySet().iterator();

                    label30:
                    do {
                        while (var8.hasNext()) {
                            Map.Entry<String, JsonElement> entry = (Map.Entry) var8.next();
                            Message.Builder entryBuilder = builder.newBuilderForField(field);
                            Object key = this.parseFieldValue(keyField, new JsonPrimitive((String) entry.getKey()), entryBuilder);
                            Object value = this.parseFieldValue(valueField, (JsonElement) entry.getValue(), entryBuilder);
                            if (value == null) {
                                continue label30;
                            }

                            entryBuilder.setField(keyField, key);
                            entryBuilder.setField(valueField, value);
                            builder.addRepeatedField(field, entryBuilder.build());
                        }

                        return;
                    } while (this.ignoringUnknownFields && valueField.getType() == Descriptors.FieldDescriptor.Type.ENUM);

                    throw new InvalidProtocolBufferException("Map value cannot be null.");
                } else {
                    throw new InvalidProtocolBufferException("Invalid map field: " + field.getFullName());
                }
            }
        }

        private void mergeOneofField(Descriptors.FieldDescriptor field, JsonElement json, Message.Builder builder) throws InvalidProtocolBufferException {
            Object value = this.parseFieldValue(field, json, builder);
            if (value != null) {
                if (builder.getOneofFieldDescriptor(field.getContainingOneof()) != null) {
                    throw new InvalidProtocolBufferException("Cannot set field " + field.getFullName() + " because another field " + builder.getOneofFieldDescriptor(field.getContainingOneof()).getFullName() + " belonging to the same oneof has already been set ");
                } else {
                    builder.setField(field, value);
                }
            }
        }

        private void mergeRepeatedField(Descriptors.FieldDescriptor field, JsonElement json, Message.Builder builder) throws InvalidProtocolBufferException {
            if (!(json instanceof JsonArray)) {
                throw new InvalidProtocolBufferException("Expected an array for " + field.getName() + " but found " + json);
            } else {
                JsonArray array = (JsonArray) json;

                for (int i = 0; i < array.size(); ++i) {
                    Object value = this.parseFieldValue(field, array.get(i), builder);
                    if (value == null) {
                        if (!this.ignoringUnknownFields || field.getType() != Descriptors.FieldDescriptor.Type.ENUM) {
                            throw new InvalidProtocolBufferException("Repeated field elements cannot be null in field: " + field.getFullName());
                        }
                    } else {
                        builder.addRepeatedField(field, value);
                    }
                }

            }
        }

        private int parseInt32(JsonElement json) throws InvalidProtocolBufferException {
            try {
                return Integer.parseInt(json.getAsString());
            } catch (RuntimeException var5) {
                try {
                    BigDecimal value = new BigDecimal(json.getAsString());
                    return value.intValueExact();
                } catch (RuntimeException var4) {
                    InvalidProtocolBufferException ex = new InvalidProtocolBufferException("Not an int32 value: " + json);
                    ex.initCause(var4);
                    throw ex;
                }
            }
        }

        private long parseInt64(JsonElement json) throws InvalidProtocolBufferException {
            try {
                return Long.parseLong(json.getAsString());
            } catch (RuntimeException var5) {
                try {
                    BigDecimal value = new BigDecimal(json.getAsString());
                    return value.longValueExact();
                } catch (RuntimeException var4) {
                    InvalidProtocolBufferException ex = new InvalidProtocolBufferException("Not an int64 value: " + json);
                    ex.initCause(var4);
                    throw ex;
                }
            }
        }

        private int parseUint32(JsonElement json) throws InvalidProtocolBufferException {
            try {
                long result = Long.parseLong(json.getAsString());
                if (result >= 0L && result <= 4294967295L) {
                    return (int) result;
                } else {
                    throw new InvalidProtocolBufferException("Out of range uint32 value: " + json);
                }
            } catch (RuntimeException var5) {
                try {
                    BigDecimal decimalValue = new BigDecimal(json.getAsString());
                    BigInteger value = decimalValue.toBigIntegerExact();
                    if (value.signum() >= 0 && value.compareTo(new BigInteger("FFFFFFFF", 16)) <= 0) {
                        return value.intValue();
                    } else {
                        throw new InvalidProtocolBufferException("Out of range uint32 value: " + json);
                    }
                } catch (RuntimeException var4) {
                    InvalidProtocolBufferException ex = new InvalidProtocolBufferException("Not an uint32 value: " + json);
                    ex.initCause(var4);
                    throw ex;
                }
            }
        }

        private long parseUint64(JsonElement json) throws InvalidProtocolBufferException {
            try {
                BigDecimal decimalValue = new BigDecimal(json.getAsString());
                BigInteger value = decimalValue.toBigIntegerExact();
                if (value.compareTo(BigInteger.ZERO) >= 0 && value.compareTo(MAX_UINT64) <= 0) {
                    return value.longValue();
                } else {
                    throw new InvalidProtocolBufferException("Out of range uint64 value: " + json);
                }
            } catch (RuntimeException var4) {
                InvalidProtocolBufferException ex = new InvalidProtocolBufferException("Not an uint64 value: " + json);
                ex.initCause(var4);
                throw ex;
            }
        }

        private boolean parseBool(JsonElement json) throws InvalidProtocolBufferException {
            if (json.getAsString().equals("true")) {
                return true;
            } else if (json.getAsString().equals("false")) {
                return false;
            } else {
                throw new InvalidProtocolBufferException("Invalid bool value: " + json);
            }
        }

        private float parseFloat(JsonElement json) throws InvalidProtocolBufferException {
            if (json.getAsString().equals("NaN")) {
                return Float.NaN;
            } else if (json.getAsString().equals("Infinity")) {
                return Float.POSITIVE_INFINITY;
            } else if (json.getAsString().equals("-Infinity")) {
                return Float.NEGATIVE_INFINITY;
            } else {
                try {
                    double value = Double.parseDouble(json.getAsString());
                    if (!(value > 3.402826869208755E38) && !(value < -3.402826869208755E38)) {
                        return (float) value;
                    } else {
                        throw new InvalidProtocolBufferException("Out of range float value: " + json);
                    }
                } catch (RuntimeException var4) {
                    InvalidProtocolBufferException ex = new InvalidProtocolBufferException("Not a float value: " + json);
                    ex.initCause(var4);
                    throw var4;
                }
            }
        }

        private double parseDouble(JsonElement json) throws InvalidProtocolBufferException {
            if (json.getAsString().equals("NaN")) {
                return Double.NaN;
            } else if (json.getAsString().equals("Infinity")) {
                return Double.POSITIVE_INFINITY;
            } else if (json.getAsString().equals("-Infinity")) {
                return Double.NEGATIVE_INFINITY;
            } else {
                try {
                    BigDecimal value = new BigDecimal(json.getAsString());
                    if (value.compareTo(MAX_DOUBLE) <= 0 && value.compareTo(MIN_DOUBLE) >= 0) {
                        return value.doubleValue();
                    } else {
                        throw new InvalidProtocolBufferException("Out of range double value: " + json);
                    }
                } catch (RuntimeException var4) {
                    InvalidProtocolBufferException ex = new InvalidProtocolBufferException("Not a double value: " + json);
                    ex.initCause(var4);
                    throw ex;
                }
            }
        }

        private String parseString(JsonElement json) {
            return json.getAsString();
        }

        private ByteString parseBytes(JsonElement json) {
            try {
                return ByteString.copyFrom(BaseEncoding.base64().decode(json.getAsString()));
            } catch (IllegalArgumentException var3) {
                return ByteString.copyFrom(BaseEncoding.base64Url().decode(json.getAsString()));
            }
        }

        @Nullable
        private Descriptors.EnumValueDescriptor parseEnum(Descriptors.EnumDescriptor enumDescriptor, JsonElement json) throws InvalidProtocolBufferException {
            String value = json.getAsString();
            Descriptors.EnumValueDescriptor result = enumDescriptor.findValueByName(value);
            if (result == null) {
                try {
                    int numericValue = this.parseInt32(json);
                    if (enumDescriptor.isClosed()) {
                        result = enumDescriptor.findValueByNumber(numericValue);
                    } else {
                        result = enumDescriptor.findValueByNumberCreatingIfUnknown(numericValue);
                    }
                } catch (InvalidProtocolBufferException var6) {
                }

                if (result == null && !this.ignoringUnknownFields) {
                    throw new InvalidProtocolBufferException("Invalid enum value: " + value + " for enum type: " + enumDescriptor.getFullName());
                }
            }

            return result;
        }

        @Nullable
        private Object parseFieldValue(Descriptors.FieldDescriptor field, JsonElement json, Message.Builder builder) throws InvalidProtocolBufferException {
            if (json instanceof JsonNull) {
                if (field.getJavaType() == Descriptors.FieldDescriptor.JavaType.MESSAGE && field.getMessageType().getFullName().equals(Value.getDescriptor().getFullName())) {
                    Value value = Value.newBuilder().setNullValueValue(0).build();
                    return builder.newBuilderForField(field).mergeFrom(value.toByteString()).build();
                } else {
                    return field.getJavaType() == Descriptors.FieldDescriptor.JavaType.ENUM && field.getEnumType().getFullName().equals(NullValue.getDescriptor().getFullName()) ? field.getEnumType().findValueByNumber(0) : null;
                }
            } else if (json instanceof JsonObject && field.getType() != Descriptors.FieldDescriptor.Type.MESSAGE && field.getType() != Descriptors.FieldDescriptor.Type.GROUP) {
                throw new InvalidProtocolBufferException(String.format("Invalid value: %s for expected type: %s", json, field.getType()));
            } else {
                switch (field.getType()) {
                    case INT32:
                    case SINT32:
                    case SFIXED32:
                        return this.parseInt32(json);
                    case INT64:
                    case SINT64:
                    case SFIXED64:
                        return this.parseInt64(json);
                    case BOOL:
                        return this.parseBool(json);
                    case FLOAT:
                        return this.parseFloat(json);
                    case DOUBLE:
                        return this.parseDouble(json);
                    case UINT32:
                    case FIXED32:
                        return this.parseUint32(json);
                    case UINT64:
                    case FIXED64:
                        return this.parseUint64(json);
                    case STRING:
                        return this.parseString(json);
                    case BYTES:
                        return this.parseBytes(json);
                    case ENUM:
                        return this.parseEnum(field.getEnumType(), json);
                    case MESSAGE:
                    case GROUP:
                        if (this.currentDepth >= this.recursionLimit) {
                            throw new InvalidProtocolBufferException("Hit recursion limit.");
                        }

                        ++this.currentDepth;
                        Message.Builder subBuilder = builder.newBuilderForField(field);
                        this.merge(json, subBuilder);
                        --this.currentDepth;
                        return subBuilder.build();
                    default:
                        throw new InvalidProtocolBufferException("Invalid field type: " + field.getType());
                }
            }
        }

        static {
            MAX_DOUBLE = (new BigDecimal(String.valueOf(Double.MAX_VALUE))).multiply(MORE_THAN_ONE);
            MIN_DOUBLE = (new BigDecimal(String.valueOf(-1.7976931348623157E308))).multiply(MORE_THAN_ONE);
        }

        private interface WellKnownTypeParser {
            void merge(ParserImpl parser, JsonElement json, Message.Builder builder) throws InvalidProtocolBufferException;
        }
    }

    private static final class PrinterImpl {
        private final com.google.protobuf.TypeRegistry registry;
        private final TypeRegistry oldRegistry;
        private final boolean alwaysOutputDefaultValueFields;
        private final Set<Descriptors.FieldDescriptor> includingDefaultValueFields;
        private final boolean preservingProtoFieldNames;
        private final boolean printingEnumsAsInts;
        private final boolean sortingMapKeys;
        private final TextGenerator generator;
        private final Gson gson;
        private final CharSequence blankOrSpace;
        private final CharSequence blankOrNewLine;
        private static final Map<String, WellKnownTypePrinter> wellKnownTypePrinters = buildWellKnownTypePrinters();

        PrinterImpl(com.google.protobuf.TypeRegistry registry, TypeRegistry oldRegistry, boolean alwaysOutputDefaultValueFields, Set<Descriptors.FieldDescriptor> includingDefaultValueFields, boolean preservingProtoFieldNames, Appendable jsonOutput, boolean omittingInsignificantWhitespace, boolean printingEnumsAsInts, boolean sortingMapKeys) {
            this.registry = registry;
            this.oldRegistry = oldRegistry;
            this.alwaysOutputDefaultValueFields = alwaysOutputDefaultValueFields;
            this.includingDefaultValueFields = includingDefaultValueFields;
            this.preservingProtoFieldNames = preservingProtoFieldNames;
            this.printingEnumsAsInts = printingEnumsAsInts;
            this.sortingMapKeys = sortingMapKeys;
            this.gson = GsonHolder.DEFAULT_GSON;
            if (omittingInsignificantWhitespace) {
                this.generator = new CompactTextGenerator(jsonOutput);
                this.blankOrSpace = "";
                this.blankOrNewLine = "";
            } else {
                this.generator = new PrettyTextGenerator(jsonOutput);
                this.blankOrSpace = " ";
                this.blankOrNewLine = "\n";
            }

        }

        void print(MessageOrBuilder message) throws IOException {
            WellKnownTypePrinter specialPrinter = (WellKnownTypePrinter) wellKnownTypePrinters.get(message.getDescriptorForType().getFullName());
            if (specialPrinter != null) {
                specialPrinter.print(this, message);
            } else {
                this.print(message, (String) null);
            }
        }

        private static Map<String, WellKnownTypePrinter> buildWellKnownTypePrinters() {
            Map<String, WellKnownTypePrinter> printers = new HashMap();
            printers.put(Any.getDescriptor().getFullName(), new WellKnownTypePrinter() {
                public void print(PrinterImpl printer, MessageOrBuilder message) throws IOException {
                    printer.printAny(message);
                }
            });
            WellKnownTypePrinter wrappersPrinter = new WellKnownTypePrinter() {
                public void print(PrinterImpl printer, MessageOrBuilder message) throws IOException {
                    printer.printWrapper(message);
                }
            };
            printers.put(BoolValue.getDescriptor().getFullName(), wrappersPrinter);
            printers.put(Int32Value.getDescriptor().getFullName(), wrappersPrinter);
            printers.put(UInt32Value.getDescriptor().getFullName(), wrappersPrinter);
            printers.put(Int64Value.getDescriptor().getFullName(), wrappersPrinter);
            printers.put(UInt64Value.getDescriptor().getFullName(), wrappersPrinter);
            printers.put(StringValue.getDescriptor().getFullName(), wrappersPrinter);
            printers.put(BytesValue.getDescriptor().getFullName(), wrappersPrinter);
            printers.put(FloatValue.getDescriptor().getFullName(), wrappersPrinter);
            printers.put(DoubleValue.getDescriptor().getFullName(), wrappersPrinter);
            printers.put(Timestamp.getDescriptor().getFullName(), new WellKnownTypePrinter() {
                public void print(PrinterImpl printer, MessageOrBuilder message) throws IOException {
                    printer.printTimestamp(message);
                }
            });
            printers.put(Duration.getDescriptor().getFullName(), new WellKnownTypePrinter() {
                public void print(PrinterImpl printer, MessageOrBuilder message) throws IOException {
                    printer.printDuration(message);
                }
            });
            printers.put(FieldMask.getDescriptor().getFullName(), new WellKnownTypePrinter() {
                public void print(PrinterImpl printer, MessageOrBuilder message) throws IOException {
                    printer.printFieldMask(message);
                }
            });
            printers.put(Struct.getDescriptor().getFullName(), new WellKnownTypePrinter() {
                public void print(PrinterImpl printer, MessageOrBuilder message) throws IOException {
                    printer.printStruct(message);
                }
            });
            printers.put(Value.getDescriptor().getFullName(), new WellKnownTypePrinter() {
                public void print(PrinterImpl printer, MessageOrBuilder message) throws IOException {
                    printer.printValue(message);
                }
            });
            printers.put(ListValue.getDescriptor().getFullName(), new WellKnownTypePrinter() {
                public void print(PrinterImpl printer, MessageOrBuilder message) throws IOException {
                    printer.printListValue(message);
                }
            });
            return printers;
        }

        private void printAny(MessageOrBuilder message) throws IOException {
            if (Any.getDefaultInstance().equals(message)) {
                this.generator.print("{}");
            } else {
                Descriptors.Descriptor descriptor = message.getDescriptorForType();
                Descriptors.FieldDescriptor typeUrlField = descriptor.findFieldByName("type_url");
                Descriptors.FieldDescriptor valueField = descriptor.findFieldByName("value");
                if (typeUrlField != null && valueField != null && typeUrlField.getType() == Descriptors.FieldDescriptor.Type.STRING && valueField.getType() == Descriptors.FieldDescriptor.Type.BYTES) {
                    String typeUrl = (String) message.getField(typeUrlField);
                    Descriptors.Descriptor type = this.registry.getDescriptorForTypeUrl(typeUrl);
                    if (type == null) {
                        type = this.oldRegistry.getDescriptorForTypeUrl(typeUrl);
                        if (type == null) {
                            throw new InvalidProtocolBufferException("Cannot find type for url: " + typeUrl);
                        }
                    }

                    ByteString content = (ByteString) message.getField(valueField);
                    Message contentMessage = (Message) DynamicMessage.getDefaultInstance(type).getParserForType().parseFrom(content);
                    WellKnownTypePrinter printer = (WellKnownTypePrinter) wellKnownTypePrinters.get(UaJsonFormat.getTypeName(typeUrl));
                    if (printer != null) {
                        this.generator.print("{" + this.blankOrNewLine);
                        this.generator.indent();
                        this.generator.print("\"@type\":" + this.blankOrSpace + this.gson.toJson(typeUrl) + "," + this.blankOrNewLine);
                        this.generator.print("\"value\":" + this.blankOrSpace);
                        printer.print(this, contentMessage);
                        this.generator.print(this.blankOrNewLine);
                        this.generator.outdent();
                        this.generator.print("}");
                    } else {
                        this.print(contentMessage, typeUrl);
                    }

                } else {
                    throw new InvalidProtocolBufferException("Invalid Any type.");
                }
            }
        }

        private void printWrapper(MessageOrBuilder message) throws IOException {
            Descriptors.Descriptor descriptor = message.getDescriptorForType();
            Descriptors.FieldDescriptor valueField = descriptor.findFieldByName("value");
            if (valueField == null) {
                throw new InvalidProtocolBufferException("Invalid Wrapper type.");
            } else {
                this.printSingleFieldValue(valueField, message.getField(valueField));
            }
        }

        private ByteString toByteString(MessageOrBuilder message) {
            return message instanceof Message ? ((Message) message).toByteString() : ((Message.Builder) message).build().toByteString();
        }

        private void printTimestamp(MessageOrBuilder message) throws IOException {
            Timestamp value = Timestamp.parseFrom(this.toByteString(message));
            this.generator.print("\"" + Timestamps.toString(value) + "\"");
        }

        private void printDuration(MessageOrBuilder message) throws IOException {
            Duration value = Duration.parseFrom(this.toByteString(message));
            this.generator.print("\"" + Durations.toString(value) + "\"");
        }

        private void printFieldMask(MessageOrBuilder message) throws IOException {
            FieldMask value = FieldMask.parseFrom(this.toByteString(message));
            this.generator.print("\"" + FieldMaskUtil.toJsonString(value) + "\"");
        }

        private void printStruct(MessageOrBuilder message) throws IOException {
            Descriptors.Descriptor descriptor = message.getDescriptorForType();
            Descriptors.FieldDescriptor field = descriptor.findFieldByName("fields");
            if (field == null) {
                throw new InvalidProtocolBufferException("Invalid Struct type.");
            } else {
                this.printMapFieldValue(field, message.getField(field));
            }
        }

        private void printValue(MessageOrBuilder message) throws IOException {
            Map<Descriptors.FieldDescriptor, Object> fields = message.getAllFields();
            if (fields.isEmpty()) {
                this.generator.print("null");
            } else if (fields.size() != 1) {
                throw new InvalidProtocolBufferException("Invalid Value type.");
            } else {
                Map.Entry entry;
                Descriptors.FieldDescriptor field;
                for (Iterator var3 = fields.entrySet().iterator(); var3.hasNext(); this.printSingleFieldValue(field, entry.getValue())) {
                    entry = (Map.Entry) var3.next();
                    field = (Descriptors.FieldDescriptor) entry.getKey();
                    if (field.getType() == Descriptors.FieldDescriptor.Type.DOUBLE) {
                        Double doubleValue = (Double) entry.getValue();
                        if (doubleValue.isNaN() || doubleValue.isInfinite()) {
                            throw new IllegalArgumentException("google.protobuf.Value cannot encode double values for infinity or nan, because they would be parsed as a string.");
                        }
                    }
                }

            }
        }

        private void printListValue(MessageOrBuilder message) throws IOException {
            Descriptors.Descriptor descriptor = message.getDescriptorForType();
            Descriptors.FieldDescriptor field = descriptor.findFieldByName("values");
            if (field == null) {
                throw new InvalidProtocolBufferException("Invalid ListValue type.");
            } else {
                this.printRepeatedFieldValue(field, message.getField(field));
            }
        }

        private void print(MessageOrBuilder message, @Nullable String typeUrl) throws IOException {
            this.generator.print("{" + this.blankOrNewLine);
            this.generator.indent();
            boolean printedField = false;
            if (typeUrl != null) {
                this.generator.print("\"@type\":" + this.blankOrSpace + this.gson.toJson(typeUrl));
                printedField = true;
            }

            Map<Descriptors.FieldDescriptor, Object> fieldsToPrint = null;
            Iterator var5;
            if (!this.alwaysOutputDefaultValueFields && this.includingDefaultValueFields.isEmpty()) {
                fieldsToPrint = message.getAllFields();
            } else {
                fieldsToPrint = new TreeMap(message.getAllFields());
                var5 = message.getDescriptorForType().getFields().iterator();

                label66:
                while (true) {
                    Descriptors.FieldDescriptor field;
                    do {
                        do {
                            while (true) {
                                if (!var5.hasNext()) {
                                    break label66;
                                }

                                field = (Descriptors.FieldDescriptor) var5.next();
                                if (!field.isOptional()) {
                                    break;
                                }

                                if (field.getJavaType() != Descriptors.FieldDescriptor.JavaType.MESSAGE || message.hasField(field)) {
                                    Descriptors.OneofDescriptor oneof = field.getContainingOneof();
                                    if (oneof == null || message.hasField(field)) {
                                        break;
                                    }
                                }
                            }
                        } while (((Map) fieldsToPrint).containsKey(field));
                    } while (!this.alwaysOutputDefaultValueFields && !this.includingDefaultValueFields.contains(field));

                    ((Map) fieldsToPrint).put(field, message.getField(field));
                }
            }

            Map.Entry field;
            for (var5 = ((Map) fieldsToPrint).entrySet().iterator(); var5.hasNext(); this.printField((Descriptors.FieldDescriptor) field.getKey(), field.getValue())) {
                field = (Map.Entry) var5.next();
                if (printedField) {
                    this.generator.print("," + this.blankOrNewLine);
                } else {
                    printedField = true;
                }
            }

            if (printedField) {
                this.generator.print(this.blankOrNewLine);
            }

            this.generator.outdent();
            this.generator.print("}");
        }

        private void printField(Descriptors.FieldDescriptor field, Object value) throws IOException {
            if (this.preservingProtoFieldNames) {
                this.generator.print("\"" + field.getName() + "\":" + this.blankOrSpace);
            } else {
                this.generator.print("\"" + field.getJsonName() + "\":" + this.blankOrSpace);
            }

            if (field.isMapField()) {
                this.printMapFieldValue(field, value);
            } else if (field.isRepeated()) {
                this.printRepeatedFieldValue(field, value);
            } else {
                this.printSingleFieldValue(field, value);
            }

        }

        private void printRepeatedFieldValue(Descriptors.FieldDescriptor field, Object value) throws IOException {
            this.generator.print("[");
            boolean printedElement = false;

            Object element;
            for (Iterator var4 = ((List) value).iterator(); var4.hasNext(); this.printSingleFieldValue(field, element)) {
                element = var4.next();
                if (printedElement) {
                    this.generator.print("," + this.blankOrSpace);
                } else {
                    printedElement = true;
                }
            }

            this.generator.print("]");
        }

        private void printMapFieldValue(Descriptors.FieldDescriptor field, Object value) throws IOException {
            Descriptors.Descriptor type = field.getMessageType();
            Descriptors.FieldDescriptor keyField = type.findFieldByName("key");
            Descriptors.FieldDescriptor valueField = type.findFieldByName("value");
            if (keyField != null && valueField != null) {
                this.generator.print("{" + this.blankOrNewLine);
                this.generator.indent();
                Collection<Object> elements = (List) value;
                Object entryValue;
                if (this.sortingMapKeys && !((Collection) elements).isEmpty()) {
                    Comparator<Object> cmp = null;
                    if (keyField.getType() == Descriptors.FieldDescriptor.Type.STRING) {
                        cmp = new Comparator<Object>() {
                            public int compare(final Object o1, final Object o2) {
                                ByteString s1 = ByteString.copyFromUtf8((String) o1);
                                ByteString s2 = ByteString.copyFromUtf8((String) o2);
                                return ByteString.unsignedLexicographicalComparator().compare(s1, s2);
                            }
                        };
                    }

                    TreeMap<Object, Object> tm = new TreeMap(cmp);
                    Iterator var9 = ((Collection) elements).iterator();

                    while (var9.hasNext()) {
                        Object element = var9.next();
                        Message entry = (Message) element;
                        entryValue = entry.getField(keyField);
                        tm.put(entryValue, element);
                    }

                    elements = tm.values();
                }

                boolean printedElement = false;
                Iterator var14 = ((Collection) elements).iterator();

                while (var14.hasNext()) {
                    Object element = var14.next();
                    Message entry = (Message) element;
                    Object entryKey = entry.getField(keyField);
                    entryValue = entry.getField(valueField);
                    if (printedElement) {
                        this.generator.print("," + this.blankOrNewLine);
                    } else {
                        printedElement = true;
                    }

                    this.printSingleFieldValue(keyField, entryKey, true);
                    this.generator.print(":" + this.blankOrSpace);
                    this.printSingleFieldValue(valueField, entryValue);
                }

                if (printedElement) {
                    this.generator.print(this.blankOrNewLine);
                }

                this.generator.outdent();
                this.generator.print("}");
            } else {
                throw new InvalidProtocolBufferException("Invalid map field.");
            }
        }

        private void printSingleFieldValue(Descriptors.FieldDescriptor field, Object value) throws IOException {
            this.printSingleFieldValue(field, value, false);
        }

        private void printSingleFieldValue(final Descriptors.FieldDescriptor field, final Object value, boolean alwaysWithQuotes) throws IOException {
            switch (field.getType()) {
                case INT32:
                case SINT32:
                case SFIXED32:
                    if (alwaysWithQuotes) {
                        this.generator.print("\"");
                    }

                    this.generator.print(((Integer) value).toString());
                    if (alwaysWithQuotes) {
                        this.generator.print("\"");
                    }
                    break;
                case INT64:
                case SINT64:
                case SFIXED64:
                    this.generator.print("\"" + ((Long) value).toString() + "\"");
                    break;
                case BOOL:
                    if (alwaysWithQuotes) {
                        this.generator.print("\"");
                    }

                    if ((Boolean) value) {
                        this.generator.print("true");
                    } else {
                        this.generator.print("false");
                    }

                    if (alwaysWithQuotes) {
                        this.generator.print("\"");
                    }
                    break;
                case FLOAT:
                    Float floatValue = (Float) value;
                    if (floatValue.isNaN()) {
                        this.generator.print("\"NaN\"");
                    } else if (floatValue.isInfinite()) {
                        if (floatValue < 0.0F) {
                            this.generator.print("\"-Infinity\"");
                        } else {
                            this.generator.print("\"Infinity\"");
                        }
                    } else {
                        if (alwaysWithQuotes) {
                            this.generator.print("\"");
                        }

                        this.generator.print(floatValue.toString());
                        if (alwaysWithQuotes) {
                            this.generator.print("\"");
                        }
                    }
                    break;
                case DOUBLE:
                    Double doubleValue = (Double) value;
                    if (doubleValue.isNaN()) {
                        this.generator.print("\"NaN\"");
                    } else if (doubleValue.isInfinite()) {
                        if (doubleValue < 0.0) {
                            this.generator.print("\"-Infinity\"");
                        } else {
                            this.generator.print("\"Infinity\"");
                        }
                    } else {
                        if (alwaysWithQuotes) {
                            this.generator.print("\"");
                        }

                        this.generator.print(doubleValue.toString());
                        if (alwaysWithQuotes) {
                            this.generator.print("\"");
                        }
                    }
                    break;
                case UINT32:
                case FIXED32:
                    if (alwaysWithQuotes) {
                        this.generator.print("\"");
                    }

                    this.generator.print(UaJsonFormat.unsignedToString((Integer) value));
                    if (alwaysWithQuotes) {
                        this.generator.print("\"");
                    }
                    break;
                case UINT64:
                case FIXED64:
                    this.generator.print("\"" + UaJsonFormat.unsignedToString((Long) value) + "\"");
                    break;
                case STRING:
                    this.generator.print(this.gson.toJson(value));
                    break;
                case BYTES:
                    this.generator.print("\"");
                    byte[] byteArray = ((ByteString) value).toByteArray();

                    if (log.isTraceEnabled()) {
                        this.generator.print(BaseEncoding.base64().encode(byteArray));
                    } else if (log.isDebugEnabled()) {
                        String encode = BaseEncoding.base64().encode(byteArray);
                        if (encode.length() > BYTES_LIMIT) {
                            encode = encode.substring(0, BYTES_LIMIT).concat(" ...");
                        }
                        this.generator.print(encode);
                    } else {
                        this.generator.print(byteArray.length + " bytes");
                    }
                    this.generator.print("\"");
                    break;
                case ENUM:
                    if (field.getEnumType().getFullName().equals("google.protobuf.NullValue")) {
                        if (alwaysWithQuotes) {
                            this.generator.print("\"");
                        }

                        this.generator.print("null");
                        if (alwaysWithQuotes) {
                            this.generator.print("\"");
                        }
                    } else if (!this.printingEnumsAsInts && ((Descriptors.EnumValueDescriptor) value).getIndex() != -1) {
                        this.generator.print("\"" + ((Descriptors.EnumValueDescriptor) value).getName() + "\"");
                    } else {
                        this.generator.print(String.valueOf(((Descriptors.EnumValueDescriptor) value).getNumber()));
                    }
                    break;
                case MESSAGE:
                case GROUP:
                    this.print((Message) value);
            }

        }

        private interface WellKnownTypePrinter {
            void print(PrinterImpl printer, MessageOrBuilder message) throws IOException;
        }

        private static class GsonHolder {
            private static final Gson DEFAULT_GSON = (new GsonBuilder()).create();

            private GsonHolder() {
            }
        }
    }

    private static final class PrettyTextGenerator implements TextGenerator {
        private final Appendable output;
        private final StringBuilder indent;
        private boolean atStartOfLine;

        private PrettyTextGenerator(final Appendable output) {
            this.indent = new StringBuilder();
            this.atStartOfLine = true;
            this.output = output;
        }

        public void indent() {
            this.indent.append("  ");
        }

        public void outdent() {
            int length = this.indent.length();
            if (length < 2) {
                throw new IllegalArgumentException(" Outdent() without matching Indent().");
            } else {
                this.indent.delete(length - 2, length);
            }
        }

        public void print(final CharSequence text) throws IOException {
            int size = text.length();
            int pos = 0;

            for (int i = 0; i < size; ++i) {
                if (text.charAt(i) == '\n') {
                    this.write(text.subSequence(pos, i + 1));
                    pos = i + 1;
                    this.atStartOfLine = true;
                }
            }

            this.write(text.subSequence(pos, size));
        }

        private void write(final CharSequence data) throws IOException {
            if (data.length() != 0) {
                if (this.atStartOfLine) {
                    this.atStartOfLine = false;
                    this.output.append(this.indent);
                }

                this.output.append(data);
            }
        }
    }

    private static final class CompactTextGenerator implements TextGenerator {
        private final Appendable output;

        private CompactTextGenerator(final Appendable output) {
            this.output = output;
        }

        public void indent() {
        }

        public void outdent() {
        }

        public void print(final CharSequence text) throws IOException {
            this.output.append(text);
        }
    }

    interface TextGenerator {
        void indent();

        void outdent();

        void print(final CharSequence text) throws IOException;
    }

    public static class TypeRegistry {
        private final Map<String, Descriptors.Descriptor> types;

        public static TypeRegistry getEmptyTypeRegistry() {
            return EmptyTypeRegistryHolder.EMPTY;
        }

        public static Builder newBuilder() {
            return new Builder();
        }

        @Nullable
        public Descriptors.Descriptor find(String name) {
            return (Descriptors.Descriptor) this.types.get(name);
        }

        @Nullable
        Descriptors.Descriptor getDescriptorForTypeUrl(String typeUrl) throws InvalidProtocolBufferException {
            return this.find(UaJsonFormat.getTypeName(typeUrl));
        }

        private TypeRegistry(Map<String, Descriptors.Descriptor> types) {
            this.types = types;
        }

        public static class Builder {
            private final Set<String> files;
            private final Map<String, Descriptors.Descriptor> types;
            private boolean built;

            private Builder() {
                this.files = new HashSet();
                this.types = new HashMap();
                this.built = false;
            }

            @CanIgnoreReturnValue
            public Builder add(Descriptors.Descriptor messageType) {
                if (this.built) {
                    throw new IllegalStateException("A TypeRegistry.Builder can only be used once.");
                } else {
                    this.addFile(messageType.getFile());
                    return this;
                }
            }

            @CanIgnoreReturnValue
            public Builder add(Iterable<Descriptors.Descriptor> messageTypes) {
                if (this.built) {
                    throw new IllegalStateException("A TypeRegistry.Builder can only be used once.");
                } else {
                    Iterator var2 = messageTypes.iterator();

                    while (var2.hasNext()) {
                        Descriptors.Descriptor type = (Descriptors.Descriptor) var2.next();
                        this.addFile(type.getFile());
                    }

                    return this;
                }
            }

            public TypeRegistry build() {
                this.built = true;
                return new TypeRegistry(this.types);
            }

            private void addFile(Descriptors.FileDescriptor file) {
                if (this.files.add(file.getFullName())) {
                    Iterator var2 = file.getDependencies().iterator();

                    while (var2.hasNext()) {
                        Descriptors.FileDescriptor dependency = (Descriptors.FileDescriptor) var2.next();
                        this.addFile(dependency);
                    }

                    var2 = file.getMessageTypes().iterator();

                    while (var2.hasNext()) {
                        Descriptors.Descriptor message = (Descriptors.Descriptor) var2.next();
                        this.addMessage(message);
                    }

                }
            }

            private void addMessage(Descriptors.Descriptor message) {
                Iterator var2 = message.getNestedTypes().iterator();

                while (var2.hasNext()) {
                    Descriptors.Descriptor nestedType = (Descriptors.Descriptor) var2.next();
                    this.addMessage(nestedType);
                }

                if (this.types.containsKey(message.getFullName())) {
                    log.warn("Type " + message.getFullName() + " is added multiple times.");
                } else {
                    this.types.put(message.getFullName(), message);
                }
            }
        }

        private static class EmptyTypeRegistryHolder {
            private static final TypeRegistry EMPTY = new TypeRegistry(Collections.emptyMap());

            private EmptyTypeRegistryHolder() {
            }
        }
    }

    public static class Parser {
        private final com.google.protobuf.TypeRegistry registry;
        private final TypeRegistry oldRegistry;
        private final boolean ignoringUnknownFields;
        private final int recursionLimit;
        private static final int DEFAULT_RECURSION_LIMIT = 100;

        private Parser(com.google.protobuf.TypeRegistry registry, TypeRegistry oldRegistry, boolean ignoreUnknownFields, int recursionLimit) {
            this.registry = registry;
            this.oldRegistry = oldRegistry;
            this.ignoringUnknownFields = ignoreUnknownFields;
            this.recursionLimit = recursionLimit;
        }

        public Parser usingTypeRegistry(TypeRegistry oldRegistry) {
            if (this.oldRegistry == TypeRegistry.getEmptyTypeRegistry() && this.registry == com.google.protobuf.TypeRegistry.getEmptyTypeRegistry()) {
                return new Parser(com.google.protobuf.TypeRegistry.getEmptyTypeRegistry(), oldRegistry, this.ignoringUnknownFields, this.recursionLimit);
            } else {
                throw new IllegalArgumentException("Only one registry is allowed.");
            }
        }

        public Parser usingTypeRegistry(com.google.protobuf.TypeRegistry registry) {
            if (this.oldRegistry == TypeRegistry.getEmptyTypeRegistry() && this.registry == com.google.protobuf.TypeRegistry.getEmptyTypeRegistry()) {
                return new Parser(registry, this.oldRegistry, this.ignoringUnknownFields, this.recursionLimit);
            } else {
                throw new IllegalArgumentException("Only one registry is allowed.");
            }
        }

        public Parser ignoringUnknownFields() {
            return new Parser(this.registry, this.oldRegistry, true, this.recursionLimit);
        }

        public void merge(String json, Message.Builder builder) throws InvalidProtocolBufferException {
            (new ParserImpl(this.registry, this.oldRegistry, this.ignoringUnknownFields, this.recursionLimit)).merge(json, builder);
        }

        public void merge(Reader json, Message.Builder builder) throws IOException {
            (new ParserImpl(this.registry, this.oldRegistry, this.ignoringUnknownFields, this.recursionLimit)).merge(json, builder);
        }

        Parser usingRecursionLimit(int recursionLimit) {
            return new Parser(this.registry, this.oldRegistry, this.ignoringUnknownFields, recursionLimit);
        }
    }

    public static class Printer {
        private final com.google.protobuf.TypeRegistry registry;
        private final TypeRegistry oldRegistry;
        private boolean alwaysOutputDefaultValueFields;
        private Set<Descriptors.FieldDescriptor> includingDefaultValueFields;
        private final boolean preservingProtoFieldNames;
        private final boolean omittingInsignificantWhitespace;
        private final boolean printingEnumsAsInts;
        private final boolean sortingMapKeys;

        private Printer(com.google.protobuf.TypeRegistry registry, TypeRegistry oldRegistry, boolean alwaysOutputDefaultValueFields, Set<Descriptors.FieldDescriptor> includingDefaultValueFields, boolean preservingProtoFieldNames, boolean omittingInsignificantWhitespace, boolean printingEnumsAsInts, boolean sortingMapKeys) {
            this.registry = registry;
            this.oldRegistry = oldRegistry;
            this.alwaysOutputDefaultValueFields = alwaysOutputDefaultValueFields;
            this.includingDefaultValueFields = includingDefaultValueFields;
            this.preservingProtoFieldNames = preservingProtoFieldNames;
            this.omittingInsignificantWhitespace = omittingInsignificantWhitespace;
            this.printingEnumsAsInts = printingEnumsAsInts;
            this.sortingMapKeys = sortingMapKeys;
        }

        public Printer usingTypeRegistry(TypeRegistry oldRegistry) {
            if (this.oldRegistry == TypeRegistry.getEmptyTypeRegistry() && this.registry == com.google.protobuf.TypeRegistry.getEmptyTypeRegistry()) {
                return new Printer(com.google.protobuf.TypeRegistry.getEmptyTypeRegistry(), oldRegistry, this.alwaysOutputDefaultValueFields, this.includingDefaultValueFields, this.preservingProtoFieldNames, this.omittingInsignificantWhitespace, this.printingEnumsAsInts, this.sortingMapKeys);
            } else {
                throw new IllegalArgumentException("Only one registry is allowed.");
            }
        }

        public Printer usingTypeRegistry(com.google.protobuf.TypeRegistry registry) {
            if (this.oldRegistry == TypeRegistry.getEmptyTypeRegistry() && this.registry == com.google.protobuf.TypeRegistry.getEmptyTypeRegistry()) {
                return new Printer(registry, this.oldRegistry, this.alwaysOutputDefaultValueFields, this.includingDefaultValueFields, this.preservingProtoFieldNames, this.omittingInsignificantWhitespace, this.printingEnumsAsInts, this.sortingMapKeys);
            } else {
                throw new IllegalArgumentException("Only one registry is allowed.");
            }
        }

        public Printer includingDefaultValueFields() {
            this.checkUnsetIncludingDefaultValueFields();
            return new Printer(this.registry, this.oldRegistry, true, Collections.emptySet(), this.preservingProtoFieldNames, this.omittingInsignificantWhitespace, this.printingEnumsAsInts, this.sortingMapKeys);
        }

        public Printer printingEnumsAsInts() {
            this.checkUnsetPrintingEnumsAsInts();
            return new Printer(this.registry, this.oldRegistry, this.alwaysOutputDefaultValueFields, this.includingDefaultValueFields, this.preservingProtoFieldNames, this.omittingInsignificantWhitespace, true, this.sortingMapKeys);
        }

        private void checkUnsetPrintingEnumsAsInts() {
            if (this.printingEnumsAsInts) {
                throw new IllegalStateException("UJsonFormat printingEnumsAsInts has already been set.");
            }
        }

        public Printer includingDefaultValueFields(Set<Descriptors.FieldDescriptor> fieldsToAlwaysOutput) {
            Preconditions.checkArgument(null != fieldsToAlwaysOutput && !fieldsToAlwaysOutput.isEmpty(), "Non-empty Set must be supplied for includingDefaultValueFields.");
            this.checkUnsetIncludingDefaultValueFields();
            return new Printer(this.registry, this.oldRegistry, false, Collections.unmodifiableSet(new HashSet(fieldsToAlwaysOutput)), this.preservingProtoFieldNames, this.omittingInsignificantWhitespace, this.printingEnumsAsInts, this.sortingMapKeys);
        }

        private void checkUnsetIncludingDefaultValueFields() {
            if (this.alwaysOutputDefaultValueFields || !this.includingDefaultValueFields.isEmpty()) {
                throw new IllegalStateException("UJsonFormat includingDefaultValueFields has already been set.");
            }
        }

        public Printer preservingProtoFieldNames() {
            return new Printer(this.registry, this.oldRegistry, this.alwaysOutputDefaultValueFields, this.includingDefaultValueFields, true, this.omittingInsignificantWhitespace, this.printingEnumsAsInts, this.sortingMapKeys);
        }

        public Printer omittingInsignificantWhitespace() {
            return new Printer(this.registry, this.oldRegistry, this.alwaysOutputDefaultValueFields, this.includingDefaultValueFields, this.preservingProtoFieldNames, true, this.printingEnumsAsInts, this.sortingMapKeys);
        }

        public Printer sortingMapKeys() {
            return new Printer(this.registry, this.oldRegistry, this.alwaysOutputDefaultValueFields, this.includingDefaultValueFields, this.preservingProtoFieldNames, this.omittingInsignificantWhitespace, this.printingEnumsAsInts, true);
        }

        public void appendTo(MessageOrBuilder message, Appendable output) throws IOException {
            (new PrinterImpl(this.registry, this.oldRegistry, this.alwaysOutputDefaultValueFields, this.includingDefaultValueFields, this.preservingProtoFieldNames, output, this.omittingInsignificantWhitespace, this.printingEnumsAsInts, this.sortingMapKeys)).print(message);
        }

        public String print(MessageOrBuilder message) throws InvalidProtocolBufferException {
            try {
                StringBuilder builder = new StringBuilder();
                this.appendTo(message, builder);
                return builder.toString();
            } catch (InvalidProtocolBufferException var3) {
                throw var3;
            } catch (IOException var4) {
                throw new IllegalStateException(var4);
            }
        }
    }
}
