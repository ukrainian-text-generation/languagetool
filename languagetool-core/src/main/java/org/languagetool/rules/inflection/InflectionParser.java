package org.languagetool.rules.inflection;

import org.languagetool.AnalyzedToken;

import java.util.Arrays;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InflectionParser {

    private final Map<String, Pattern> inflectionToRegexMap;

    public InflectionParser(Map<String, Pattern> inflectionToRegexMap) {

        this.inflectionToRegexMap = inflectionToRegexMap;
    }

    public Inflection createInflection(AnalyzedToken token) {

        final Inflection result = new Inflection();
        String posTag = token.getPOSTag();

        if (posTag == null) throw new IllegalArgumentException("POS tag of the token is null");

        inflectionToRegexMap
                .forEach((key, value) -> addInflectionFromEntry(key, value, posTag, result));

        final String restOfTheTag = result.getAllValues().stream()
                .reduce(posTag, (tag, value) -> tag.replace(value, ""))
                .replaceAll(":{2,}", ":");

        Arrays.stream(restOfTheTag.split(":"))
                .forEach(tag -> result.addInflection(tag, Boolean.TRUE));

        return result;
    }

    private static void addInflectionFromEntry(String key, Pattern pattern, String posTag,
            Inflection inflection) {

        String posTagInternal = posTag;

        Matcher matcher = pattern.matcher(posTagInternal);

        while (matcher.find()) {

            final String value = matcher.group(key);
            inflection.addInflection(key, value);

            posTagInternal = posTagInternal.replace(value, "");
            matcher = pattern.matcher(posTagInternal);
        }

    }
}
