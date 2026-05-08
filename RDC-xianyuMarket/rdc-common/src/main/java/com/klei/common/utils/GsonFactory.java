package com.klei.common.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class GsonFactory {

    private static final Gson INSTANCE = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new TypeAdapter<LocalDateTime>() {
                @Override
                public void write(JsonWriter out, LocalDateTime value) throws IOException {
                    out.value(value != null ? value.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null);
                }

                @Override
                public LocalDateTime read(JsonReader in) throws IOException {
                    String str = in.nextString();
                    return str != null && !str.isEmpty() ? LocalDateTime.parse(str, DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null;
                }
            })
            .create();

    public static Gson get() {
        return INSTANCE;
    }
}
