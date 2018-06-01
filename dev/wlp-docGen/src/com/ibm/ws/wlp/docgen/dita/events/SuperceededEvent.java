package com.ibm.ws.wlp.docgen.dita.events;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import javax.xml.stream.XMLStreamException;

import com.ibm.ws.wlp.docgen.dita.DITAHelper;

public class SuperceededEvent extends FeatureEvent implements Event {

  public SuperceededEvent(String feature) {
    super("feat_supersededby", feature);
  }

  @Override
  public void prepareEvents(DITAHelper writer)
      throws XMLStreamException, IOException {
    super.prepareEvents(writer);
  }

  @Override
  protected void addFeature(String feature) {
  }

  @Override
  protected Map<String, String> getTags(DITAHelper writer) {
    return Collections.emptyMap();
  }

}
