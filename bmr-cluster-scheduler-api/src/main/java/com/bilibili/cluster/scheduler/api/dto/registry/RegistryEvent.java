package com.bilibili.cluster.scheduler.api.dto.registry;

public class RegistryEvent {

    // The prefix which is watched
    private String key;
    // The full path where the event was generated
    private String path;
    // The value corresponding to the path
    private String data;
    // The event type {ADD, REMOVE, UPDATE}
    private Type type;

    public RegistryEvent(String key, String path, String data, Type type) {
        this.key = key;
        this.path = path;
        this.data = data;
        this.type = type;
    }

    public RegistryEvent() {
    }

    public static EventBuilder builder() {
        return new EventBuilder();
    }

    public String key() {
        return this.key;
    }

    public String path() {
        return this.path;
    }

    public String data() {
        return this.data;
    }

    public Type type() {
        return this.type;
    }

    public RegistryEvent key(String key) {
        this.key = key;
        return this;
    }

    public RegistryEvent path(String path) {
        this.path = path;
        return this;
    }

    public RegistryEvent data(String data) {
        this.data = data;
        return this;
    }

    public RegistryEvent type(Type type) {
        this.type = type;
        return this;
    }

    public String toString() {
        return "Event(key=" + this.key() + ", path=" + this.path() + ", data=" + this.data() + ", type=" + this.type()
                + ")";
    }

    public enum Type {
        ADD,
        REMOVE,
        UPDATE
    }

    public static class EventBuilder {

        private String key;
        private String path;
        private String data;
        private Type type;

        EventBuilder() {
        }

        public EventBuilder key(String key) {
            this.key = key;
            return this;
        }

        public EventBuilder path(String path) {
            this.path = path;
            return this;
        }

        public EventBuilder data(String data) {
            this.data = data;
            return this;
        }

        public EventBuilder type(Type type) {
            this.type = type;
            return this;
        }

        public RegistryEvent build() {
            return new RegistryEvent(key, path, data, type);
        }

        public String toString() {
            return "Event.EventBuilder(key=" + this.key + ", path=" + this.path + ", data=" + this.data + ", type="
                    + this.type + ")";
        }
    }
}
