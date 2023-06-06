package uk.org.lidalia.slf4jtest;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import org.slf4j.event.Level;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.fail;
import static org.slf4j.event.Level.DEBUG;
import static org.slf4j.event.Level.INFO;
import static uk.org.lidalia.slf4jtest.LoggingEvent.info;

public class TestLoggerFactoryResetRuleUnitTests {

    TestLoggerFactoryResetRule resetRule = new TestLoggerFactoryResetRule();

    @Test
    public void resetsThreadLocalDataBeforeTest() throws Throwable {

        final TestLogger logger = TestLoggerFactory.getTestLogger("logger_name");
        logger.setEnabledLevels(INFO, DEBUG);
        logger.info("a message");

        resetRule.apply(new Statement() {
            @Override
            public void evaluate() throws Throwable {
            	assertThat(TestLoggerFactory.getLoggingEvents(), is(Collections.<LoggingEvent>emptyList()));
            	assertThat(logger.getLoggingEvents(), is(Collections.<LoggingEvent>emptyList()));
            	assertThat(logger.getEnabledLevels(), is(EnumSet.allOf(Level.class)));
            }
        }, Description.EMPTY).evaluate();
    }
    
    @Test
    public void resetsThreadLocalDataAfterTest() throws Throwable {

        final TestLogger logger = TestLoggerFactory.getTestLogger("logger_name");
        logger.setEnabledLevels(INFO, DEBUG);
        logger.info("a message");

        resetRule.apply(new Statement() {
            @Override
            public void evaluate() throws Throwable {
            }
        }, Description.EMPTY).evaluate();


        assertThat(TestLoggerFactory.getLoggingEvents(), is(Collections.<LoggingEvent>emptyList()));
        assertThat(logger.getLoggingEvents(), is(Collections.<LoggingEvent>emptyList()));
        assertThat(logger.getEnabledLevels(), is(EnumSet.allOf(Level.class)));
    }

    @Test
    public void resetsThreadLocalDataOnException() throws Throwable {

        final TestLogger logger = TestLoggerFactory.getTestLogger("logger_name");
        logger.setEnabledLevels(INFO, DEBUG);
        logger.info("a message");

        final Exception toThrow = new Exception();
        Exception thrown=null;
        try{
            resetRule.apply(new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    throw toThrow;
                }
            }, Description.EMPTY).evaluate();
            fail("No exception thrown");
        }
        catch(Exception e){
            thrown=e;
        }
        assertThat(thrown, is(toThrow));
        assertThat(TestLoggerFactory.getLoggingEvents(), is(Collections.<LoggingEvent>emptyList()));
        assertThat(logger.getLoggingEvents(), is(Collections.<LoggingEvent>emptyList()));
        assertThat(logger.getEnabledLevels(), is(EnumSet.allOf(Level.class)));
    }

    @Test
    public void doesNotResetNonThreadLocalData() throws Throwable {

        final TestLogger logger = TestLoggerFactory.getTestLogger("logger_name");
        logger.info("a message");

        resetRule.apply(new Statement() {
            @Override
            public void evaluate() throws Throwable {
            }
        }, Description.EMPTY).evaluate();

        final List<LoggingEvent> loggedEvents = asList(info("a message"));

        assertThat(TestLoggerFactory.getAllLoggingEvents(), is(loggedEvents));
        assertThat(logger.getAllLoggingEvents(), is(loggedEvents));
    }

    @Before @After
    public void resetTestLoggerFactory() {
        TestLoggerFactory.reset();
    }
}
