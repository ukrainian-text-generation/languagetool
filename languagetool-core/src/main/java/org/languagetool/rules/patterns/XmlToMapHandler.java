package org.languagetool.rules.patterns;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

public class XmlToMapHandler extends DefaultHandler {

  private static final String CHARACTERS_KEY = "$characters";

  private final Map<Object, Object> root = new HashMap<>();
  private Map<Object, Object> parentNode;
  private Deque<Map<Object, Object>> stack = new LinkedList<>(Collections.singleton(root));

  @Override
  public void startElement(final String uri, final String localName, final String qName, final Attributes attributes) {

    final Map<Object, Object> currentNode = stack.peekFirst();

    if (currentNode == null) throw new RuntimeException();

    Map<Object, Object> newNode = new HashMap<>();

    IntStream.range(0, attributes.getLength())
      .boxed()
      .forEach(i -> newNode.put(attributes.getLocalName(i), attributes.getValue(i)));

    addAttributeToNode(currentNode, qName, newNode);

    stack.push(newNode);
  }

  @Override
  public void endElement(final String uri, final String localName, final String qName) {

    stack.removeFirst();
  }

  @Override
  public void characters(final char[] ch, final int start, final int length) {

    final Map<Object, Object> currentNode = stack.peekFirst();

    if (currentNode == null) throw new RuntimeException();

    addAttributeToNode(currentNode, CHARACTERS_KEY, String.valueOf(ch).substring(start, start + length));
  }

  public Map<Object, Object> getResult() {

    return root;
  }

  private void addAttributeToNode(Map<Object, Object> node, String attributeName, Object value) {

    Object valueForKey = node.get(attributeName);

    if (valueForKey == null) {

      node.put(attributeName, value);

    } else {

      List<Object> newList = new ArrayList<>();

      if (valueForKey instanceof Collection) newList.addAll((Collection<Object>) valueForKey);
      else newList.add(valueForKey);

      newList.add(value);
      node.put(attributeName, newList);
    }
  }
}
