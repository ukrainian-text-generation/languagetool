package org.languagetool.rules.patterns;

import org.json.JSONArray;
import org.json.JSONObject;
import org.languagetool.Language;
import org.languagetool.rules.Category;
import org.languagetool.rules.CategoryId;
import org.languagetool.rules.CorrectExample;
import org.languagetool.rules.ErrorTriggeringExample;
import org.languagetool.rules.ExampleSentence;
import org.languagetool.rules.IncorrectExample;
import org.languagetool.rules.dependency.DependencyBasedRule;
import org.languagetool.rules.inflection.InflectionParser;
import org.languagetool.synthesis.Synthesizer;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class DependencyBasedRuleHandler extends XMLRuleHandler {

    private static final Map<String, Function<String, ExampleSentence>> CORRECTION_TO_EXAMPLE_INITIATOR_MAP = new HashMap<>();

    static {
        CORRECTION_TO_EXAMPLE_INITIATOR_MAP.put("correct", CorrectExample::new);
        CORRECTION_TO_EXAMPLE_INITIATOR_MAP.put("incorrect", IncorrectExample::new);
        CORRECTION_TO_EXAMPLE_INITIATOR_MAP.put("triggers_error", ErrorTriggeringExample::new);
    }

    private static final Function<String, ExampleSentence> DEFAULT_EXAMPLE_INITIATOR = CorrectExample::new;

    private final String fileName;
    private final Language language;
    private final XmlToMapHandler xmlToMapHandler = new XmlToMapHandler();
    private final InflectionParser inflectionParser;
    private final Synthesizer synthesizer;

    public DependencyBasedRuleHandler(final String filename, final Language lang, InflectionParser inflectionParser,
            Synthesizer synthesizer) {
        this.fileName = filename;
        this.language = lang;
        this.inflectionParser = inflectionParser;
        this.synthesizer = synthesizer;
    }

    @Override
    public void startElement(final String uri, final String localName, final String qName,
            final Attributes attributes) throws SAXException {

        xmlToMapHandler.startElement(uri, localName, qName, attributes);
    }

    @Override
    public void endElement(final String uri, final String localName, final String qName) throws SAXException {

        xmlToMapHandler.endElement(uri, localName, qName);
    }

    @Override
    public void characters(final char[] ch, final int start, final int length) throws SAXException {

        xmlToMapHandler.characters(ch, start, length);
    }

    public List<DependencyBasedRule> getDependencyBasedRules() {

        final Map<Object, Object> result = xmlToMapHandler.getResult();

        final JSONObject jsonResult = new JSONObject(result);

        return getRulesForCategories(jsonResult.getJSONObject("rules").getJSONObject("category"));
    }

    private List<DependencyBasedRule> getRulesForCategories(Object categories) {

        if (categories instanceof JSONArray) {

            JSONArray categoriesArray = (JSONArray) categories;
            return categoriesArray.toList().stream()
                    .filter(JSONObject.class::isInstance)
                    .map(JSONObject.class::cast)
                    .map(this::getRulesForCategory)
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList());
        } else {

            return getRulesForCategory((JSONObject) categories);
        }
    }

    private List<DependencyBasedRule> getRulesForCategory(JSONObject jsonCategory) {

        final Object rules = jsonCategory.get("rule");
        final Category category = createCategory(jsonCategory);

        if (rules instanceof JSONArray) {

            JSONArray rulesArray = (JSONArray) rules;

            return StreamSupport.stream(Spliterators.spliteratorUnknownSize(rulesArray.iterator(), Spliterator.ORDERED), false)
                    .filter(JSONObject.class::isInstance)
                    .map(JSONObject.class::cast)
                    .map(rule -> createRuleFromJson(rule, category))
                    .collect(Collectors.toList());
        } else {

            return Collections.singletonList(createRuleFromJson((JSONObject) rules, category));
        }

    }

    private Category createCategory(JSONObject category) {

        return new Category(new CategoryId(category.getString("id")), category.getString("name"));
    }

    private DependencyBasedRule createRuleFromJson(JSONObject jsonRule, Category category) {

        final DependencyBasedRule rule = new DependencyBasedRule(jsonRule.getString("id"), jsonRule.getString("name"), language, inflectionParser, synthesizer);

        rule.setCategory(category);
        rule.setMessage(jsonRule.getJSONObject("message").getString("$characters"));

        final Map<Class<? extends ExampleSentence>, List<ExampleSentence>> examplesForRule = createExamplesForRule(jsonRule);

        rule.setCorrectExamples(getExamplesForClass(examplesForRule.get(CorrectExample.class), CorrectExample.class));
        rule.setIncorrectExamples(getExamplesForClass(examplesForRule.get(IncorrectExample.class), IncorrectExample.class));
        rule.setErrorTriggeringExamples(getExamplesForClass(examplesForRule.get(ErrorTriggeringExample.class), ErrorTriggeringExample.class));

        rule.setQuery(jsonRule.getJSONObject("dependency").getJSONObject("query").getString("$characters"));
        rule.setValidation(jsonRule.getJSONObject("dependency").getJSONObject("validation").getString("$characters"));
        rule.setCorrection(jsonRule.getJSONObject("dependency").getJSONObject("correction").getString("$characters"));

        return rule;
    }

    private Map<Class<? extends ExampleSentence>, List<ExampleSentence>> createExamplesForRule(JSONObject rule) {

        final Object examples = rule.get("example");

        if (examples instanceof JSONArray) {

            JSONArray examplesArray = (JSONArray) examples;

            return StreamSupport.stream(Spliterators.spliteratorUnknownSize(examplesArray.iterator(), Spliterator.ORDERED), false)
                    .filter(JSONObject.class::isInstance)
                    .map(JSONObject.class::cast)
                    .map(this::createExampleFromJson)
                    .collect(Collectors.groupingBy(ExampleSentence::getClass));
        } else {

            ExampleSentence exampleSentence = createExampleFromJson((JSONObject) examples);

            return Collections.singletonMap(exampleSentence.getClass(), Collections.singletonList(exampleSentence));
        }
    }

    private <T> List<T> getExamplesForClass(List<ExampleSentence> examples, Class<T> tClass) {

        return Optional.ofNullable(examples)
                .orElse(Collections.emptyList())
                .stream()
                .filter(tClass::isInstance)
                .map(tClass::cast)
                .collect(Collectors.toList());
    }

    private ExampleSentence createExampleFromJson(JSONObject example) {

        return Optional.of(example)
                .filter(e -> e.has("correction"))
                .map(e -> e.getString("correction"))
                .map(correction -> CORRECTION_TO_EXAMPLE_INITIATOR_MAP.getOrDefault(correction, DEFAULT_EXAMPLE_INITIATOR))
                .orElse(DEFAULT_EXAMPLE_INITIATOR)
                .apply(example.getString("$characters"));
    }
}
