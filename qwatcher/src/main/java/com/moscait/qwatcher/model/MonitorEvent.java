package com.moscait.qwatcher.model;

public record MonitorEvent(long timestamp, String type, String message) {
    public static final String TYPE_INFO = "INFO";
    public static final String TYPE_WARNING = "WARNING";
    public static final String TYPE_ERROR = "ERROR";
    public static final String TYPE_SUCCESS = "SUCCESS";
}
