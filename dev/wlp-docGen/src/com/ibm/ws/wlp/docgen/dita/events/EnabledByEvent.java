package com.ibm.ws.wlp.docgen.dita.events;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.ibm.ws.wlp.docgen.dita.DITAHelper;

public class EnabledByEvent extends FeatureEvent {
  private static List<String> features = new ArrayList<String>();

  public EnabledByEvent(String feature) {
    super("feat_enabledby", feature);
  }

  @Override
  protected void addFeature(String feature) {
    features.add(feature);
  }

  @Override
  protected Map<String, String> getTags(DITAHelper writer) {
    Map<String, String> tags = writer.getTags(features);
    features.clear();
    return tags;
  }
}
