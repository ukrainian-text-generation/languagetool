package org.languagetool.rules.patterns;

import org.languagetool.Language;
import org.languagetool.rules.dependency.DependencyBasedRule;
import org.languagetool.rules.inflection.InflectionParser;
import org.languagetool.synthesis.Synthesizer;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class DependencyBasedRuleLoader extends DefaultHandler {

  public final List<DependencyBasedRule> getRules(InputStream is, String filename, Language language, InflectionParser inflectionParser, Synthesizer synthesizer) throws IOException {
    List<DependencyBasedRule> rules;
    try {
      DependencyBasedRuleHandler handler = new DependencyBasedRuleHandler(filename, language, inflectionParser, synthesizer);
      SAXParserFactory factory = SAXParserFactory.newInstance();
      SAXParser saxParser = factory.newSAXParser();
      saxParser.getXMLReader().setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
      saxParser.parse(is, handler);
      rules = handler.getDependencyBasedRules();
      return rules;
    } catch (Exception e) {
      throw new IOException("Cannot load or parse '" + filename + "'", e);
    }
  }
}
