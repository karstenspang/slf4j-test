package uk.org.lidalia.slf4jtest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.ILoggerFactory;
import org.slf4j.event.Level;


public final class TestLoggerFactory implements ILoggerFactory {

    private static TestLoggerFactory INSTANCE = null;

    public static synchronized TestLoggerFactory getInstance() {
        if (INSTANCE==null) INSTANCE=createTestLoggerFactory();
        return INSTANCE;
    }

    public static TestLogger getTestLogger(final Class<?> aClass) {
        return getInstance().getLogger(aClass);
    }

    public static TestLogger getTestLogger(final String name) {
        return getInstance().getLogger(name);
    }

    public static Map<String, TestLogger> getAllTestLoggers() {
        return getInstance().getAllLoggers();
    }

    public static void clear() {
        getInstance().clearLoggers();
    }

    public static void clearAll() {
        getInstance().clearAllLoggers();
    }

    static void reset() {
        getInstance().doReset();
    }

    public static List<LoggingEvent> getLoggingEvents() {
        return getInstance().getLoggingEventsFromLoggers();
    }

    public static List<LoggingEvent> getAllLoggingEvents() {
        return getInstance().getAllLoggingEventsFromLoggers();
    }

    private final ConcurrentMap<String, TestLogger> loggers = new ConcurrentHashMap<>();
    private final List<LoggingEvent> allLoggingEvents = new CopyOnWriteArrayList<>();
    private volatile ThreadLocal<List<LoggingEvent>> loggingEvents =
            ThreadLocal.withInitial(Suppliers.<LoggingEvent>makeEmptyMutableList());
    private volatile Level printLevel;

    public TestLoggerFactory() {
        this(null);
    }

    /**
     * @param printLevel maximum print level. If {@code null}, printing is off.
     */
    public TestLoggerFactory(Level printLevel) {
        this.printLevel = printLevel;
    }

    /**
     * @return the maximum level to print. if {@code null}, nothing is printed.
     */
    public Level getPrintLevel() {
        return printLevel;
    }

    public Map<String, TestLogger> getAllLoggers() {
        return Collections.unmodifiableMap(new HashMap<>(loggers));
    }

    public TestLogger getLogger(final Class<?> aClass) {
        return getLogger(aClass.getName());
    }

    public TestLogger getLogger(final String name) {
        return loggers.computeIfAbsent(name,n->new TestLogger(n, this));
    }

    public void clearLoggers() {
        for (final TestLogger testLogger: loggers.values()) {
            testLogger.clear();
        }
        loggingEvents.get().clear();
    }

    public void clearAllLoggers() {
        for (final TestLogger testLogger: loggers.values()) {
            testLogger.clearAll();
        }
        loggingEvents =
            ThreadLocal.withInitial(Suppliers.<LoggingEvent>makeEmptyMutableList());
        allLoggingEvents.clear();
    }

    void doReset() {
        clearAllLoggers();
        loggers.clear();
    }

    public List<LoggingEvent> getLoggingEventsFromLoggers() {
        return Collections.unmodifiableList(new ArrayList<>(loggingEvents.get()));
    }

    public List<LoggingEvent> getAllLoggingEventsFromLoggers() {
        return allLoggingEvents;
    }

    void addLoggingEvent(final LoggingEvent event) {
        loggingEvents.get().add(event);
        allLoggingEvents.add(event);
    }

    /**
     * Set the maximum print level.
     * @param printLevel maximum print level to set. If {@code null}, printing is off.
     */
    public void setPrintLevel(final Level printLevel) {
        this.printLevel =printLevel;
    }

    private static TestLoggerFactory createTestLoggerFactory(){
        try {
            final String level = new OverridableProperties("slf4jtest").getProperty("print.level", "OFF");
            Level printLevel;
            if ("OFF".equals(level)){
                printLevel=null;
            }
            else{
                printLevel=Level.valueOf(level);
            }
            return new TestLoggerFactory(printLevel);
        }
        catch (IllegalArgumentException e) {
            throw new IllegalStateException("Invalid level name in property print.level of file slf4jtest.properties " +
                    "or System property slf4jtest.print.level", e);
        }
        catch(IOException e){
            throw new IllegalStateException("Error reading file slf4jtest.properties.",e);
        }
    }
}
