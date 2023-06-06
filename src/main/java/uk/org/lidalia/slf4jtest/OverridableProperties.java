package uk.org.lidalia.slf4jtest;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

class OverridableProperties {
    private static final Properties EMPTY_PROPERTIES = new Properties();
    private final String propertySourceName;
    private final Properties properties;

    OverridableProperties(final String propertySourceName) throws IOException {
        this.propertySourceName = propertySourceName;
        this.properties = getProperties();
    }

    private Properties getProperties() throws IOException {
       InputStream stream = Thread.currentThread().getContextClassLoader()
            .getResourceAsStream(propertySourceName + ".properties");
        if (stream==null) return EMPTY_PROPERTIES;
        return loadProperties(stream);
    }

    private static Properties loadProperties(final InputStream propertyResource)
        throws IOException
    {
        try (InputStream closablePropertyResource = propertyResource) {
            final Properties loadedProperties = new Properties();
            loadedProperties.load(closablePropertyResource);
            return loadedProperties;
        }
    }

    public String getProperty(final String propertyKey, final String defaultValue) {
        final String propertyFileProperty = properties.getProperty(propertyKey, defaultValue);
        return System.getProperty(propertySourceName + "." + propertyKey, propertyFileProperty);
    }
}
