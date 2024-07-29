package org.languagetool.integration.dto;

public class TokenDto {

  private String value;
  private int index;
  private String dependency;
  private int parentIndex;

  public String getValue() {
    return value;
  }

  public void setValue(final String value) {
    this.value = value;
  }

  public int getIndex() {
    return index;
  }

  public void setIndex(final int index) {
    this.index = index;
  }

  public String getDependency() {
    return dependency;
  }

  public void setDependency(final String dependency) {
    this.dependency = dependency;
  }

  public int getParentIndex() {
    return parentIndex;
  }

  public void setParentIndex(final int parentIndex) {
    this.parentIndex = parentIndex;
  }
}

//return {
//  "value": token.text,
//  "index": token.i,
//  "dependency": token.dep_,
//  "parentIndex": token.head.i
//    }
