package com.uangel.ccaas.msggw.util;

import com.github.javaparser.utils.StringEscapeUtils;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageOrBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class JsonUtil {
    private static final UaJsonFormat.Printer jsonPrinter = UaJsonFormat.printer().includingDefaultValueFields();

    private JsonUtil() {
        // nothing
    }

    public static String printMessage(MessageOrBuilder message) throws InvalidProtocolBufferException {
        String json = jsonPrinter.print(message);
        return StringEscapeUtils.unescapeJava(json);
    }


    public static Optional<String> json2String(String jsonStr, String... elements) {
        try {
            return json2JsonElement(jsonStr, elements)
                    .map(JsonElement::getAsString);
        } catch (Exception e) {
            return Optional.empty();
        }
    }


    public static Optional<Integer> json2Int(String jsonStr, String... elements) {
        try {
            return json2JsonElement(jsonStr, elements)
                    .map(JsonElement::getAsInt);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public static Optional<JsonElement> json2JsonElement(String jsonStr, String... element) {
        Optional<JsonObject> optionalJsonObject = Optional.ofNullable(new Gson().fromJson(jsonStr, JsonObject.class));
        for (AtomicInteger i = new AtomicInteger(); i.get() < element.length - 1; i.getAndIncrement()) {
            optionalJsonObject = optionalJsonObject.map(o -> o.get(element[i.get()]))
                    .map(JsonElement::getAsJsonObject);
        }
        return optionalJsonObject.map(o -> o.get(element[element.length - 1]));
    }

    public static String toJson(Object object) {
        return new Gson().toJson(object);
    }

    public static JsonObject jsonStr2JsonObj(String jsonStr) {
        return new Gson().fromJson(jsonStr, JsonObject.class);
    }

    // HttpResponseObject => Class<T> 변환
    public static <T> T checkObjectResp(String resObject, Class<T> responseType) {
        if (resObject != null) {
            return JsonUtil.fromJson(resObject, responseType);
        } else {
            return null;
        }
    }

    public static <T> T fromJson(String json, Class<T> classOfT) {
        return new Gson().fromJson(json, classOfT);
    }


}
