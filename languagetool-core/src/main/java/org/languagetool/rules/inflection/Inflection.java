package org.languagetool.rules.inflection;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Inflection {

    private final Map<String, Object> inflections = new HashMap<>();

    public void addInflection(String tag, Object value) {

        inflections.put(tag, value);
    }

    public Object getInflection(String tag) {

        return inflections.get(tag);
    }

    public List<String> getAllValues() {

        return inflections.values().stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .collect(Collectors.toList());
    }

    public Collection<Map.Entry<String, Object>> getInflections() {

        return inflections.entrySet();
    }
}
