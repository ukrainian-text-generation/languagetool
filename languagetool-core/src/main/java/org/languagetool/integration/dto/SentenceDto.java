package org.languagetool.integration.dto;

import java.util.List;

public class SentenceDto {
  private List<TokenDto> tokens;

  public List<TokenDto> getTokens() {
    return tokens;
  }

  public void setTokens(final List<TokenDto> tokens) {
    this.tokens = tokens;
  }
}
