package uk.org.lidalia.slf4jtest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.MDC;
import org.slf4j.Marker;
import org.slf4j.event.Level;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.LegacyAbstractLogger;
import org.slf4j.helpers.MessageFormatter;

import static java.util.Arrays.asList;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static org.slf4j.event.Level.DEBUG;
import static org.slf4j.event.Level.ERROR;
import static org.slf4j.event.Level.INFO;
import static org.slf4j.event.Level.TRACE;
import static org.slf4j.event.Level.WARN;

/**
 * <p>
 * Implementation of {@link Logger} which stores {@link LoggingEvent}s in memory and provides methods
 * to access and remove them in order to facilitate writing tests that assert particular logging calls were made.
 * </p>
 * <p>
 * {@link LoggingEvent}s are stored in both an {@link ThreadLocal} and a normal {@link List}. The {@link #getLoggingEvents()}
 * and {@link #clear()} methods reference the {@link ThreadLocal} events. The {@link #getAllLoggingEvents()} and
 * {@link #clearAll()} methods reference all events logged on this Logger.  This is in order to facilitate parallelising
 * tests - tests that use the thread local methods can be parallelised.
 * </p>
 * <p>
 * By default all Levels are enabled.  It is important to note that the conventional hierarchical notion of Levels, where
 * info being enabled implies warn and error being enabled, is not a requirement of the SLF4J API, so the
 * {@link #setEnabledLevels(Collection)}, {@link #setEnabledLevels(Level...)},
 * {@link #setEnabledLevelsForAllThreads(Collection)}, {@link #setEnabledLevelsForAllThreads(Level...)} and the various
 * isXxxxxEnabled() methods make no assumptions about this hierarchy.
 * </p>
 */
@SuppressWarnings({ "PMD.ExcessivePublicCount", "PMD.TooManyMethods" })
public class TestLogger extends LegacyAbstractLogger {
    private final static Set<Level> allLevels=Collections.unmodifiableSet(EnumSet.allOf(Level.class));

    private final String name;
    private final TestLoggerFactory testLoggerFactory;
    private volatile ThreadLocal<List<LoggingEvent>> loggingEvents = ThreadLocal.withInitial(
            Suppliers.<LoggingEvent>makeEmptyMutableList());

    private final List<LoggingEvent> allLoggingEvents = new CopyOnWriteArrayList<>();
    private volatile ThreadLocal<Set<Level>> enabledLevels =ThreadLocal.withInitial(()->allLevels);

    TestLogger(final String name, final TestLoggerFactory testLoggerFactory) {
        this.name = name;
        this.testLoggerFactory = testLoggerFactory;
    }

    public String getName() {
        return name;
    }

    /**
     * Removes all {@link LoggingEvent}s logged by this thread and resets the enabled levels of the logger
     * to all levels for this thread.
     */
    public void clear() {
        loggingEvents.get().clear();
        enabledLevels.set(allLevels);
    }

    /**
     * Removes ALL {@link LoggingEvent}s logged on this logger, regardless of thread,
     * and resets the enabled levels of the logger to all levels.
     * for ALL threads.
     */
    public void clearAll() {
        allLoggingEvents.clear();
        loggingEvents = ThreadLocal.withInitial(
            Suppliers.<LoggingEvent>makeEmptyMutableList());
        enabledLevels = ThreadLocal.withInitial(()->allLevels);
    }

    /**
     * @return all {@link LoggingEvent}s logged on this logger by this thread
     */
    public List<LoggingEvent> getLoggingEvents() {
        return Collections.unmodifiableList(new ArrayList<>(loggingEvents.get()));
    }

    /**
     * @return all {@link LoggingEvent}s logged on this logger by ANY thread
     */
    public List<LoggingEvent> getAllLoggingEvents() {
        return Collections.unmodifiableList(new ArrayList<>(allLoggingEvents));
    }

    /**
     * @return whether this logger is trace enabled in this thread
     */
    @Override
    public boolean isTraceEnabled() {
        return enabledLevels.get().contains(TRACE);
    }

    /**
     * @return whether this logger is debug enabled in this thread
     */
    @Override
    public boolean isDebugEnabled() {
        return enabledLevels.get().contains(DEBUG);
    }

    /**
     * @return whether this logger is info enabled in this thread
     */
    @Override
    public boolean isInfoEnabled() {
        return enabledLevels.get().contains(INFO);
    }

    /**
     * @return whether this logger is warn enabled in this thread
     */
    @Override
    public boolean isWarnEnabled() {
        return enabledLevels.get().contains(WARN);
    }

    /**
     * @return whether this logger is error enabled in this thread
     */
    @Override
    public boolean isErrorEnabled() {
        return enabledLevels.get().contains(ERROR);
    }
    @Override

    protected void handleNormalizedLoggingCall(Level level, Marker marker, String msg, Object[] arguments, Throwable throwable)
    {
        addLoggingEvent(level,ofNullable(marker),ofNullable(throwable),msg,arguments);
    }

    private void addLoggingEvent(
            final Level level,
            final Optional<Marker> marker,
            final Optional<Throwable> throwable,
            final String format,
            final Object[] args) {
            final LoggingEvent event = new LoggingEvent(of(this), level, mdc(), marker, throwable, format, args);
            allLoggingEvents.add(event);
            loggingEvents.get().add(event);
            testLoggerFactory.addLoggingEvent(event);
            optionallyPrint(event);
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> mdc() {
        Map<String, String> mdc=MDC.getCopyOfContextMap();
        return mdc;
    }

    private void optionallyPrint(final LoggingEvent event) {
        Level printLevel=testLoggerFactory.getPrintLevel();
        if (printLevel!=null && printLevel.compareTo(event.getLevel()) >= 0) {
            event.print();
        }
    }

    /**
     * @return the set of levels enabled for this logger on this thread
     */
    public Set<Level> getEnabledLevels() {
        return enabledLevels.get();
    }

    /**
     * The conventional hierarchical notion of Levels, where info being enabled implies warn and error being enabled, is not a
     * requirement of the SLF4J API, so all levels you wish to enable must be passed explicitly to this method.
     *
     * @param enabledLevels levels which will be considered enabled for this logger IN THIS THREAD;
     *                      does not affect enabled levels for this logger in other threads
     */
    public void setEnabledLevels(final Collection<Level> enabledLevels) {
        this.enabledLevels.set(createImmutableLevelSet(enabledLevels));
    }

    /**
     * The conventional hierarchical notion of Levels, where info being enabled implies warn and error being enabled, is not a
     * requirement of the SLF4J API, so all levels you wish to enable must be passed explicitly to this method.
     *
     * @param enabledLevels levels which will be considered enabled for this logger IN THIS THREAD;
     *                      does not affect enabled levels for this logger in other threads
     */
    public void setEnabledLevels(final Level... enabledLevels) {
        setEnabledLevels(asList(enabledLevels));
    }

    /**
     * The conventional hierarchical notion of Levels, where info being enabled implies warn and error being enabled, is not a
     * requirement of the SLF4J API, so all levels you wish to enable must be passed explicitly to this method.
     *
     * @param enabledLevelsForAllThreads levels which will be considered enabled for this logger IN ALL THREADS
     */
    public void setEnabledLevelsForAllThreads(final Collection<Level> enabledLevelsForAllThreads) {
        final Set<Level> enabledSet=createImmutableLevelSet(enabledLevelsForAllThreads);
        this.enabledLevels = ThreadLocal.withInitial(()->enabledSet);
    }

    /**
     * The conventional hierarchical notion of Levels, where info being enabled implies warn and error being enabled, is not a
     * requirement of the SLF4J API, so all levels you wish to enable must be passed explicitly to this method.
     
     * @param enabledLevelsForAllThreads levels which will be considered enabled for this logger IN ALL THREADS
     */
    public void setEnabledLevelsForAllThreads(final Level... enabledLevelsForAllThreads) {
        setEnabledLevelsForAllThreads(asList(enabledLevelsForAllThreads));
    }
    
    private Set<Level> createImmutableLevelSet(final Collection<Level> enabledLevels){
        Set<Level> enabledSet;
        if (enabledLevels.isEmpty()){
            enabledSet=EnumSet.noneOf(Level.class);
        }
        else{
            enabledSet=EnumSet.copyOf(enabledLevels);
        }
        return Collections.unmodifiableSet(enabledSet);
    }

    @Override
    protected String getFullyQualifiedCallerName() {
        return null;
    }
}
