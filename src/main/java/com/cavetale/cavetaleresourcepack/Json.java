package com.cavetale.cavetaleresourcepack;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public final class Json {
    public static final Gson GSON = new GsonBuilder().create();
    public static final Gson PRETTY = new GsonBuilder().setPrettyPrinting().create();
    private static final Pattern SIMPLE = Pattern.compile("[0-9a-zA-Z_-]+");

    private Json() { }

    public static <T> T load(final File file, Class<T> type, Supplier<T> dfl) {
        if (!file.exists()) {
            return dfl.get();
        }
        try (FileReader fr = new FileReader(file)) {
            return GSON.fromJson(fr, type);
        } catch (FileNotFoundException fnfr) {
            return dfl.get();
        } catch (IOException ioe) {
            throw new IllegalStateException("Loading " + file, ioe);
        }
    }

    public static <T> T load(final File file, Class<T> type) {
        return load(file, type, () -> null);
    }

    public static void save(final File file, Object obj, boolean pretty) {
        try (FileWriter fw = new FileWriter(file)) {
            Gson gs = pretty ? PRETTY : GSON;
            gs.toJson(obj, fw);
        } catch (IOException ioe) {
            throw new IllegalStateException("Saving " + file, ioe);
        }
    }

    public static void save(final File file, Object obj) {
        save(file, obj, false);
    }

    public static String serialize(Object obj) {
        return GSON.toJson(obj);
    }

    public static <T> T deserialize(String json, Class<T> type) {
        return GSON.fromJson(json, type);
    }
}
