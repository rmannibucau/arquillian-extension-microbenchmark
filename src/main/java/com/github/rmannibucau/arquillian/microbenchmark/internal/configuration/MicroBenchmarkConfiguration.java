package com.github.rmannibucau.arquillian.microbenchmark.internal.configuration;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Properties;

public class MicroBenchmarkConfiguration {
    public static final String CONFIGURATION_PATH = "microbenchmark.properties";

    private boolean activated = true;
    private boolean detailed = true;

    // diagrams specific config
    private int width = 800;
    private int height = 600;
    private boolean saveDiagram = false;
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

        readFromMap(props);
    }

    public void readFromMap(final Map<?, ?> extensionProperties) {
        if (extensionProperties == null) {
            return;
        }

        for (final Field field : getClass().getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }

            field.setAccessible(true);
            final String name = field.getName();
            if (extensionProperties.containsKey(name)) {
                try {
                    field.set(this, fromString(field.getType(), extensionProperties.get(name)));
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

    public boolean isActivated() {
        return activated;
    }

    public void setActivated(final boolean activated) {
        this.activated = activated;
    }

    public void setWidth(final int width) {
        this.width = width;
    }

    public void setHeight(final int height) {
        this.height = height;
    }

    public void setSaveDiagram(final boolean saveDiagram) {
        this.saveDiagram = saveDiagram;
    }

    public void setDiagramFolder(final String diagramFolder) {
        this.diagramFolder = diagramFolder;
    }

    public boolean isDetailed() {
        return detailed;
    }

    public void setDetailed(final boolean detailed) {
        this.detailed = detailed;
    }

    private static String toString(final Object o) {
        return "" + o;
    }

    private Object fromString(final Class<?> type, final Object property) {
        if (!String.class.isInstance(property)) {
            return property;
        }

        if (Boolean.TYPE.equals(type)) {
            return Boolean.parseBoolean(String.class.cast(property));
        }
        if (Integer.TYPE.equals(type)) {
            return Integer.parseInt(String.class.cast(property));
        }
        return property;
    }
}
