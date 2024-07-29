package org.languagetool.tagging;

public class DependencyParsingInfo {

  private String dependency;

  private int index;
  private int parentIndex;

  public String getDependency() {
    return dependency;
  }

  public void setDependency(final String dependency) {
    this.dependency = dependency;
  }

  public int getIndex() {
    return index;
  }

  public void setIndex(final int index) {
    this.index = index;
  }

  public int getParentIndex() {
    return parentIndex;
  }

  public void setParentIndex(final int parentIndex) {
    this.parentIndex = parentIndex;
  }
}
