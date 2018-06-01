package com.ibm.ws.wlp.docgen.dita.events;

import java.util.Collections;
import java.util.Map;

import com.ibm.ws.wlp.docgen.dita.DITAHelper;

public class EnablesEvent extends FeatureEvent implements Event {

  public EnablesEvent(String feature) {
    super("feat_enables", feature);
  }

  @Override
  protected void addFeature(String feature) {
  }

  @Override
  protected Map<String, String> getTags(DITAHelper writer) {
    return Collections.emptyMap();
  }

}
