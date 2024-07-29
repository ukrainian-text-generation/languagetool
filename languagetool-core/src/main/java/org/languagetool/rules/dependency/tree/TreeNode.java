package org.languagetool.rules.dependency.tree;

import org.languagetool.AnalyzedToken;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TreeNode {

  private final AnalyzedToken token;
  private final Map<String, List<TreeNode>> childNodes;

  public TreeNode(AnalyzedToken token, Map<String, List<TreeNode>> childNodes) {

    this.token = token;
    this.childNodes = childNodes;
  }

  public AnalyzedToken getToken() {

    return token;
  }

  public List<TreeNode> getNodesForDependency(String dependency) {

    return childNodes.computeIfAbsent(dependency, (dep) -> new ArrayList<>());
  }
}
