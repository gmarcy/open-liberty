package com.ibm.ws.wlp.docgen.dita.events;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.XMLStreamException;

import com.ibm.ws.wlp.docgen.dita.DITAHelper;
import com.ibm.ws.wlp.docgen.dita.Feature;
import com.ibm.ws.wlp.docgen.dita.XMLWriter;

public abstract class FeatureEvent extends SimpleEvent {
  private String type;
  private String feature;
  private static Map<String, String> names = new HashMap<String, String>();
  
  public FeatureEvent(String type, String feature) {
    this.type = type;
    this.feature = feature;
    addFeature(feature);
  }
  
  protected abstract void addFeature(String feature);
  protected abstract Map<String, String> getTags(DITAHelper writer);
  
  @Override
  public void processEvent(DITAHelper writer)
      throws XMLStreamException, IOException {
    XMLWriter xml = writer.getWriter();
    String featureName = feature;
    if (featureName.charAt(0) == '[') {
      featureName = featureName.substring(1, featureName.length() - 1);
    }
    xml.pushElement("li");
    
    Map<String, String> tags = writer.getTags(featureName);
    for (Map.Entry<String, String> tag : tags.entrySet()) {
      xml.addAttribute(tag.getKey(), tag.getValue());
    }
    
    xml.pushElement("xref");
    xml.addAttribute("href", "rwlp_feature_" + featureName + ".dita");
    String displayName = names.get(featureName);
    String text = featureName + (displayName == null ? "" : " - " + displayName);
    xml.addText(text);
    xml.pop("xref"); // xref
    xml.pop("li"); // li
  }

  @Override
  public void prepareEvents(DITAHelper writer)
      throws XMLStreamException, IOException {
    XMLWriter xml = writer.getWriter();
    xml.pushElement("section");
    
    Map<String, String> tags = getTags(writer);
    for (Map.Entry<String, String> tag : tags.entrySet()) {
      xml.addAttribute(tag.getKey(), tag.getValue());
    }
    
    xml.pushElement("title");
    xml.pushElement("ph");
    xml.addAttribute("conref", DITAHelper.IDCONTROLLED_ENTITIES_PREFIX + "" + type);
    xml.pop("ph"); // ph
    xml.pop("title"); // title
    xml.pushElement("ul");
  }

  @Override
  public void postpareEvents(DITAHelper writer)
      throws XMLStreamException, IOException {
    XMLWriter xml = writer.getWriter();
    xml.pop("ul"); // ul
    xml.pop("section"); // section
  }

  @Override
  public int compareTo(Event o) {
    return feature.compareTo(((FeatureEvent)o).feature);
  }
  
  public static void addFeatures(Collection<Feature> features) {
    names.clear();
    for (Feature f : features) {
      names.put(f.name, f.displayName);
    }
  }
}
