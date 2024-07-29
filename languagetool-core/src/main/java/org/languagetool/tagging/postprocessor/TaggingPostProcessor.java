package org.languagetool.tagging.postprocessor;

import org.languagetool.AnalyzedTokenReadings;

import java.util.List;

public interface TaggingPostProcessor {

  List<AnalyzedTokenReadings> postProcess(List<AnalyzedTokenReadings> tokens);
}
