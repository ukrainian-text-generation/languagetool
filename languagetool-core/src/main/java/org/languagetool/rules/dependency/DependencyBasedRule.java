package org.languagetool.rules.dependency;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlExpression;
import org.languagetool.AnalyzedSentence;
import org.languagetool.AnalyzedToken;
import org.languagetool.AnalyzedTokenReadings;
import org.languagetool.Language;
import org.languagetool.rules.RuleMatch;
import org.languagetool.rules.dependency.el.ValidationEvaluationContext;
import org.languagetool.rules.dependency.tree.Tree;
import org.languagetool.rules.inflection.Inflection;
import org.languagetool.rules.inflection.InflectionParser;
import org.languagetool.rules.patterns.AbstractPatternRule;
import org.languagetool.synthesis.Synthesizer;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class DependencyBasedRule extends AbstractPatternRule {

  private final InflectionParser inflectionParser;
  private final Synthesizer synthesizer;

  @Setter
  @Getter
  private String query;

  @Setter
  @Getter
  private String validation;

  @Setter
  @Getter
  private String correction;


  public DependencyBasedRule(final String id, final String description, final Language language,
                             InflectionParser inflectionParser, Synthesizer synthesizer) {

    super(id, description, language);
    this.inflectionParser = inflectionParser;
    this.synthesizer = synthesizer;
  }

  @Override
  public RuleMatch[] match(final AnalyzedSentence sentence) throws IOException {

    try {

      return matchInternal(sentence);

    } catch (Exception e) {

      return RuleMatch.EMPTY_ARRAY;
    }
  }

  public RuleMatch[] matchInternal(final AnalyzedSentence sentence) throws IOException {

    final Tree dependencyTree = new Tree(sentence);

    final Map<AnalyzedToken, Inflection> tokenToInflection = Arrays.stream(sentence.getTokens())
      .map(token -> token.getAnalyzedToken(0))
      .filter(token -> token.getPOSTag() != null)
      .collect(Collectors.toMap(Function.identity(), inflectionParser::createInflection));

    final List<List<AnalyzedToken>> paths = dependencyTree.query(getQueryDependencies());

    final List<String> tokenNames = getTokenNames();

    final List<List<AnalyzedToken>> invalidPaths = paths.stream()
      .filter(path -> !isPathValid(path, tokenNames, tokenToInflection))
      .collect(Collectors.toList());

    final Map<AnalyzedToken, String[]> corrections = invalidPaths.stream()
      .map(path -> getPathCorrections(path, tokenNames, tokenToInflection))
      .map(Map::entrySet)
      .flatMap(Collection::stream)
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (v1, v2) -> v1));

    return getRuleMatches(sentence, corrections);
  }

  private List<String> getQueryDependencies() {

    return Arrays.stream(query.split("->"))
      .filter(str -> str.startsWith("[") && str.endsWith("]"))
      .map(str -> str.replace("[", "").replace("]", ""))
      .collect(Collectors.toList());
  }

  private List<String> getTokenNames() {

    return Arrays.stream(query.split("->"))
      .filter(str -> !str.startsWith("[") && !str.endsWith("]"))
      .collect(Collectors.toList());
  }

  private boolean isPathValid(List<AnalyzedToken> path, List<String> tokenNames,
                              Map<AnalyzedToken, Inflection> tokenToInflection) {

    return (Boolean) createExpression(validation)
      .evaluate(createContext(path, tokenNames, tokenToInflection));
  }

  private JexlExpression createExpression(String expression) {

    return new JexlBuilder()
      .silent(Boolean.TRUE)
      .create()
      .createExpression(expression);
  }

  private ValidationEvaluationContext createContext(List<AnalyzedToken> path, List<String> tokenNames,
                                                    Map<AnalyzedToken, Inflection> tokenToInflection) {

    ValidationEvaluationContext context = new ValidationEvaluationContext();

    IntStream.range(0, path.size())
      .boxed()
      .forEach(index -> tokenToInflection.get(path.get(index))
        .getInflections()
        .forEach(inf -> context.setUntracked(tokenNames.get(index) + "_" + inf.getKey(),
          inf.getValue())));

    return context;
  }

  private Map<AnalyzedToken, String[]> getPathCorrections(List<AnalyzedToken> path, List<String> tokenNames,
                                                          Map<AnalyzedToken, Inflection> tokenToInflection) {

    final JexlExpression expression = createExpression(correction);

    final ValidationEvaluationContext context = createContext(path, tokenNames, tokenToInflection);

    expression.evaluate(context);

    final Map<String, List<String>> tokenToChangedTag = context.getChangedVariables().stream()
      .map(variable -> {
        String[] split = variable.split("_");
        return new AbstractMap.SimpleEntry<>(split[0], split[1]);
      })
      .collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.mapping(Map.Entry::getValue, Collectors.toList())));

    return tokenToChangedTag.entrySet().stream()
      .map(entry -> getTokenSuggestion(path, tokenNames, tokenToInflection, entry.getKey(), entry.getValue(), context))
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  @SneakyThrows
  private Map.Entry<AnalyzedToken, String[]> getTokenSuggestion(List<AnalyzedToken> path, List<String> tokenNames,
                                                                Map<AnalyzedToken, Inflection> tokenToInflection,
                                                                String tokenName, List<String> changedTags,
                                                                JexlContext context) {

    final int index = tokenNames.indexOf(tokenName);

    final AnalyzedToken token = path.get(index);
    final Inflection inflection = tokenToInflection.get(token);

    final String newPosTag = changedTags.stream()
      .reduce(token.getPOSTag(),
        (t, ct) -> updatePosTag(t, ct, inflection.getInflection(ct), context.get(tokenName + "_" + ct)));

    return new AbstractMap.SimpleEntry<>(token, synthesizer.synthesize(token, newPosTag));
  }

  private String updatePosTag(String currentPosTagValue, String tagName, Object previousValue, Object newValue) {

    String newTag = currentPosTagValue;

    if (newValue == null) return currentPosTagValue;

    if (previousValue == null) {

      if (newValue instanceof Boolean && newValue.equals(Boolean.TRUE)) {

        newTag = currentPosTagValue + ":" + tagName;

      } else if (newValue instanceof String) {

        newTag = currentPosTagValue + ":" + newValue;
      }

    } else {

      if (newValue instanceof Boolean && newValue.equals(Boolean.FALSE)) {

        String pattern = "(?<=^|:)" + Pattern.quote(tagName) + "(?=$|:)";

        newTag = currentPosTagValue.replaceAll(pattern, "");

      } else if (newValue instanceof String && previousValue instanceof String) {

        String pattern = "(?<=^|:)" + Pattern.quote((String) previousValue) + "(?=$|:)";

        newTag = currentPosTagValue.replaceAll(pattern, (String) newValue);
      }
    }

    return newTag.replaceAll(":{2,}", ":");
  }

  private RuleMatch[] getRuleMatches(AnalyzedSentence sentence, Map<AnalyzedToken, String[]> corrections) {

    return corrections.entrySet()
      .stream()
      .map(entry -> getRuleMatch(sentence, entry.getKey(), entry.getValue()))
      .filter(Optional::isPresent)
      .map(Optional::get)
      .toArray(RuleMatch[]::new);
  }

  private Optional<RuleMatch> getRuleMatch(AnalyzedSentence sentence, AnalyzedToken token, String[] suggestions) {

    return getReadingsForToken(sentence, token)
      .map(reading -> new RuleMatch(this, sentence, reading.getStartPos(), reading.getEndPos(), message, message,
        Arrays.asList(suggestions)));
  }

  private Optional<AnalyzedTokenReadings> getReadingsForToken(AnalyzedSentence sentence, AnalyzedToken token) {

    return Arrays.stream(sentence.getTokens())
      .filter(reading -> reading.getReadings().contains(token))
      .findFirst();
  }
}
