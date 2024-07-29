package org.languagetool.integration.dto;

import java.util.List;

public class DependencyParsingDto {

  private List<SentenceDto> sentences;

  public List<SentenceDto> getSentences() {
    return sentences;
  }

  public void setSentences(final List<SentenceDto> sentences) {
    this.sentences = sentences;
  }
}
