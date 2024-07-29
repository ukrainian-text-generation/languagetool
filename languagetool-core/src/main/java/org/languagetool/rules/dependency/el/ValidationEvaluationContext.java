package org.languagetool.rules.dependency.el;

import org.apache.commons.jexl3.MapContext;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ValidationEvaluationContext extends MapContext {

    private final Set<String> changedVariables = new HashSet<>();

    @Override
    public Object get(final String name) {

        Object result = super.get(name);

        return result != null ? result : Boolean.FALSE;
    }

    public void setUntracked(final String name, final Object value) {

        super.set(name, value);
    }

    @Override
    public void set(final String name, final Object value) {

        changedVariables.add(name);
        super.set(name, value);
    }

    public Set<String> getChangedVariables() {

        return changedVariables;
    }
}
