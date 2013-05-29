package com.github.rmannibucau.arquillian.microbenchmark.internal.configuration;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Properties;

public class MicroBenchmarkConfiguration {
    public static final String CONFIGURATION_PATH = "microbenchmark.properties";

    private int width = 800;
    private int height = 600;
    private boolean saveDiagram = true;
    private String diagramFolder = "target/micro-benchmark";

    public String asString() {
        final StringBuilder builder = new StringBuilder();
        for (final Field field : getClass().getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }

            field.setAccessible(true);
            try {
                builder.append(field.getName()).append(" = ").append(toString(field.get(this))).append("\n");
            } catch (final IllegalAccessException e) {
                // no-op
            }
        }
        return builder.toString();
    }

    public void initFromInputStream(final InputStream inputStream) {
        final Properties props = new Properties();
        try {
            props.load(inputStream);
        } catch (final IOException e) {
            // no-op
        }

        for (final Field field : getClass().getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }

            field.setAccessible(true);
            final String name = field.getName();
            if (props.containsKey(name)) {
                try {
                    field.set(this, fromString(field.getType(), props.getProperty(name)));
                } catch (final IllegalAccessException e) {
                    // no-op
                }
            }
        }
    }

    public String getDiagramFolder() {
        return diagramFolder;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean isSaveDiagram() {
        return saveDiagram;
    }

    private static String toString(final Object o) {
        return "" + o;
    }

    private Object fromString(final Class<?> type, final String property) {
        if (Boolean.TYPE.equals(type)) {
            return Boolean.parseBoolean(property);
        }
        if (Integer.TYPE.equals(type)) {
            return Integer.parseInt(property);
        }
        return property;
    }
}
