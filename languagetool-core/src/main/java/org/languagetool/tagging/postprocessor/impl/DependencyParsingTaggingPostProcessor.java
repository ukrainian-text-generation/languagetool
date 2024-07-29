package org.languagetool.tagging.postprocessor.impl;

import org.languagetool.AnalyzedToken;
import org.languagetool.AnalyzedTokenReadings;
import org.languagetool.integration.RestIntegrationService;
import org.languagetool.integration.dto.DependencyParsingDto;
import org.languagetool.integration.dto.SentenceDto;
import org.languagetool.integration.dto.TokenDto;
import org.languagetool.tagging.DependencyParsingInfo;
import org.languagetool.tagging.postprocessor.TaggingPostProcessor;
import org.languagetool.tools.config.ConfigService;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.languagetool.constans.LanguageToolConstants.Config.DEPENDENCY_PARSING_URL;

public class DependencyParsingTaggingPostProcessor implements TaggingPostProcessor {

  private final RestIntegrationService<DependencyParsingDto, DependencyParsingDto> integrationService =
    new RestIntegrationService<>();
  private final ConfigService configService = new ConfigService();

  @Override
  public List<AnalyzedTokenReadings> postProcess(List<AnalyzedTokenReadings> tokens) {

    final DependencyParsingDto request = createRequest(tokens);

    String url = configService.getProperty(DEPENDENCY_PARSING_URL);

    final DependencyParsingDto response = integrationService.post(url, request, DependencyParsingDto.class);

    if (tokens.size() != response.getSentences().get(0).getTokens().size())
      throw new RuntimeException("Token qty mismatch");

    return IntStream.range(0, tokens.size())
      .boxed()
      .map(index -> populateDependencyInfo(tokens.get(index), response.getSentences().get(0).getTokens().get(index)))
      .collect(Collectors.toList());
  }

  private DependencyParsingDto createRequest(List<AnalyzedTokenReadings> tokens) {

    final DependencyParsingDto dependencyParsingDto = new DependencyParsingDto();
    final SentenceDto sentenceDto = new SentenceDto();
    dependencyParsingDto.setSentences(Collections.singletonList(sentenceDto));

    sentenceDto.setTokens(tokens.stream().map(this::createToken).collect(Collectors.toList()));

    return dependencyParsingDto;
  }

  private TokenDto createToken(AnalyzedTokenReadings token) {

    final TokenDto result = new TokenDto();
    result.setValue(token.getToken());

    return result;
  }

  private AnalyzedTokenReadings populateDependencyInfo(AnalyzedTokenReadings sourceToken, TokenDto parsedToken) {

    final AnalyzedToken analyzedToken = sourceToken.getAnalyzedToken(0);

    final DependencyParsingInfo dependencyParsingInfo = new DependencyParsingInfo();
    dependencyParsingInfo.setDependency(parsedToken.getDependency());
    dependencyParsingInfo.setIndex(parsedToken.getIndex());
    dependencyParsingInfo.setParentIndex(parsedToken.getParentIndex());

    analyzedToken.setDependencyParsingInfo(dependencyParsingInfo);

    return sourceToken;
  }
}
