package uk.org.lidalia.slf4jtest;

import org.junit.Test;
import org.slf4j.MarkerFactory;
import org.slf4j.helpers.BasicMarkerFactory;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;
import org.slf4j.IMarkerFactory;
import org.slf4j.MarkerFactory;
import org.slf4j.MDC;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;

public class TestServiceProviderTests {

    @Test
    public void getMarkerFactory() throws Exception {
        assertSame(BasicMarkerFactory.class, MarkerFactory.getIMarkerFactory().getClass());
        assertSame(MarkerFactory.getIMarkerFactory(),MarkerFactory.getIMarkerFactory());
    }

    @Test
    public void getLoggerFactoryReturnsCorrectlyFromSlf4JLoggerFactory() {
        ILoggerFactory expected = TestLoggerFactory.getInstance();
        assertSame(expected,LoggerFactory.getILoggerFactory());
    }

    @Test
    public void requestedApiVersion() throws Exception {
        assertEquals("2.0.99", new TestServiceProvider().getRequestedApiVersion());
    }

    @Test
    public void getMDCAdapter() throws Exception {
        assertSame(TestMDCAdapter.class, MDC.getMDCAdapter().getClass());
        assertSame(MDC.getMDCAdapter(),MDC.getMDCAdapter());
    }
}
