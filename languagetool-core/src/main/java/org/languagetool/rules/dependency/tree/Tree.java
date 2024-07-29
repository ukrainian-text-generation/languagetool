package org.languagetool.rules.dependency.tree;

import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.languagetool.AnalyzedSentence;
import org.languagetool.AnalyzedToken;
import org.languagetool.tagging.DependencyParsingInfo;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Tree {

  private final TreeNode root;
  private final List<TreeNode> allNodes;

  public Tree(AnalyzedSentence sentence) {

    final Map<AnalyzedToken, DependencyParsingInfo> tokenToInfoMap = Arrays.stream(sentence.getTokens())
      .map(token -> token.getAnalyzedToken(0))
      .collect(Collectors.toMap(Function.identity(), this::getDependencyInfo, (existing, replacement) -> replacement));

    final Map<Integer, TreeNode> indexToNodeMap = getIndexToNodeMap(tokenToInfoMap);

    this.root = getRootNode(tokenToInfoMap);

    indexToNodeMap.put(tokenToInfoMap.get(root.getToken()).getIndex(), root);

    this.allNodes = new ArrayList<>(indexToNodeMap.values());

    assignDependenciesBetweenNodes(tokenToInfoMap, indexToNodeMap);
  }

  public List<List<AnalyzedToken>> query(List<String> dependencies) {

    return allNodes.stream()
      .flatMap(node -> query(node, dependencies).orElse(Collections.emptyList()).stream())
      .map(path -> path.stream().map(TreeNode::getToken).collect(Collectors.toList()))
      .collect(Collectors.toList());
  }

  private Optional<List<List<TreeNode>>> query(TreeNode currentNode, List<String> dependencies) {

    if (dependencies.isEmpty()) return Optional.of(Collections.singletonList(Collections.singletonList(currentNode)));

    List<String> nextDependencies = dependencies.subList(1, dependencies.size());
    String currentDependency = dependencies.get(0);

    return Optional.ofNullable(currentNode.getNodesForDependency(currentDependency))
      .filter(collection -> !collection.isEmpty())
      .map(nodes -> nodes.stream()
        .flatMap(node -> query(node, nextDependencies).orElse(Collections.emptyList()).stream())
        .map(path -> Stream.concat(Stream.of(currentNode), path.stream()).collect(Collectors.toList()))
        .collect(Collectors.toList()))
      ;
  }

  @SneakyThrows
  private DependencyParsingInfo getDependencyInfo(AnalyzedToken token) {

    return Optional.ofNullable(token.getDependencyParsingInfo())
      .orElseGet(DependencyParsingInfo::new);
  }

  private Map<Integer, TreeNode> getIndexToNodeMap(Map<AnalyzedToken, DependencyParsingInfo> tokenToInfoMap) {

    return tokenToInfoMap.entrySet().stream()
      .filter(this::isValidChildDependencyInfo)
      .map(entry -> new AbstractMap.SimpleImmutableEntry<>(entry.getValue().getIndex(),
        new TreeNode(entry.getKey(), new HashMap<>())))
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private TreeNode getRootNode(Map<AnalyzedToken, DependencyParsingInfo> tokenToInfoMap) {

    return tokenToInfoMap.entrySet().stream()
      .filter(this::isValidRootDependencyInfo)
      .map(entry -> new TreeNode(entry.getKey(), new HashMap<>()))
      .findFirst()
      .orElseThrow(() -> new IllegalStateException("No ROOT node found"));
  }

  private boolean isValidChildDependencyInfo(Map.Entry<AnalyzedToken, DependencyParsingInfo> entry) {

    return StringUtils.isNotEmpty(entry.getValue().getDependency())
      && !entry.getValue().getDependency().equals("dep")
      && !entry.getValue().getDependency().equals("ROOT");
  }

  private boolean isValidRootDependencyInfo(Map.Entry<AnalyzedToken, DependencyParsingInfo> entry) {

    return StringUtils.isNotEmpty(entry.getValue().getDependency())
      && entry.getValue().getDependency().equals("ROOT");
  }

  private void assignDependenciesBetweenNodes(Map<AnalyzedToken, DependencyParsingInfo> tokenToInfoMap,
                                              Map<Integer, TreeNode> indexToNodeMap) {

    this.allNodes
      .forEach(node -> {
        final DependencyParsingInfo info = tokenToInfoMap.get(node.getToken());
        final TreeNode parent = indexToNodeMap.get(info.getParentIndex());
        parent.getNodesForDependency(info.getDependency()).add(node);
      });
  }
}
