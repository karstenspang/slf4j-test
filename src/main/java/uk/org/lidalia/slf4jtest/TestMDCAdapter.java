package uk.org.lidalia.slf4jtest;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.helpers.BasicMDCAdapter;

import static java.util.Optional.ofNullable;

public class TestMDCAdapter extends BasicMDCAdapter {

    private final ThreadLocal<Map<String, String>> value = ThreadLocal.withInitial(
            Suppliers.<String, String>makeEmptyMutableMap());

    @Override
    public void put(final String key, final String val) {
        value.get().put(key, ofNullable(val).orElse("null"));
    }

    @Override
    public String get(final String key) {
        return value.get().get(key);
    }

    @Override
    public void remove(final String key) {
        value.get().remove(key);
    }

    @Override
    public void clear() {
        value.get().clear();
    }

    @Override
    public Map<String, String> getCopyOfContextMap() {
        return Collections.unmodifiableMap(new HashMap<>(value.get()));
    }

    @Override
    public Set<String> getKeys() {
        return Collections.unmodifiableSet(new HashSet<>(value.get().keySet()));
    }

    @Override
    public void setContextMap(final Map<String,String> contextMap) {
        value.set(new HashMap<String, String>(contextMap));
    }
}
