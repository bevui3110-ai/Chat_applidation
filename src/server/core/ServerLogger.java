package server.core;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ServerLogger {
    public static class LogEntry {
        private final String time;
        private final String level; // INFO, AUTH, MSG, BROADCAST, DISCONNECT, FILE, ERROR
        private final String tag;
        private final String message;

        public LogEntry(String level, String tag, String message) {
            this.time = new SimpleDateFormat("HH:mm:ss").format(new Date());
            this.level = level;
            this.tag = tag;
            this.message = message;
        }

        public String getTime() { return time; }
        public String getLevel() { return level; }
        public String getTag() { return tag; }
        public String getMessage() { return message; }

        public String toFormattedString() {
            return String.format("[%s] [%-8s] [%s] %s", time, level, tag, message);
        }
    }

    public interface LogListener {
        void onLogAdded(LogEntry entry);
        void onLogsCleared();
    }

    private static final List<LogEntry> logs = new CopyOnWriteArrayList<>();
    private static final List<LogListener> listeners = new CopyOnWriteArrayList<>();

    public static void addListener(LogListener listener) {
        listeners.add(listener);
    }

    public static void removeListener(LogListener listener) {
        listeners.remove(listener);
    }

    public static void log(String level, String tag, String message) {
        LogEntry entry = new LogEntry(level, tag, message);
        logs.add(entry);
        System.out.println(entry.toFormattedString());
        for (LogListener l : listeners) {
            try {
                l.onLogAdded(entry);
            } catch (Exception ignored) {}
        }
    }

    public static void info(String tag, String message) { log("INFO", tag, message); }
    public static void auth(String tag, String message) { log("AUTH", tag, message); }
    public static void msg(String tag, String message) { log("MSG", tag, message); }
    public static void broadcast(String tag, String message) { log("BROADCAST", tag, message); }
    public static void disconnect(String tag, String message) { log("DISCONNECT", tag, message); }
    public static void file(String tag, String message) { log("FILE", tag, message); }
    public static void error(String tag, String message) { log("ERROR", tag, message); }

    public static List<LogEntry> getLogs() {
        return new ArrayList<>(logs);
    }

    public static void clearLogs() {
        logs.clear();
        for (LogListener l : listeners) {
            try {
                l.onLogsCleared();
            } catch (Exception ignored) {}
        }
    }
}
