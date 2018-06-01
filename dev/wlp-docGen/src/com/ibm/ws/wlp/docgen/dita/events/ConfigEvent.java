package com.ibm.ws.wlp.docgen.dita.events;

import java.util.ArrayList;
import java.util.List;

public abstract class ConfigEvent extends SimpleEvent {
  protected List<String> context;
  protected String elementName;

  public ConfigEvent(List<String> context, String name) {
    this.context = new ArrayList<String>(context);
    this.elementName = name;
  }

  protected static boolean isEndAfter(ConfigElementEvent element, ConfigElementEndEvent end) {
    
    return false;
  }
  
  protected static boolean isAttributeAfter(ConfigElementEvent element, ConfigAttributeEvent attribute) {
    return false;
  }
  
  @Override
  public int compareTo(Event o) {
    ConfigEvent other = (ConfigEvent) o;
    List<String> thisElements = new ArrayList<String>(context);
    thisElements.add(elementName);
    List<String> otherElements = new ArrayList<String>(other.context);
    otherElements.add(other.elementName);
    
    for (int i = 0; i < thisElements.size() && i < otherElements.size(); i++) {
      String thisEntry = thisElements.get(i);
      String otherEntry = otherElements.get(i);
      
      int result = thisEntry.compareTo(otherEntry);
      if (result != 0) {
        return result;
      }
    }
    
    return thisElements.size() - otherElements.size();
  }
  
  public String toString() {
    StringBuilder builder = new StringBuilder();
    
    for (String str : context) {
      builder.append(str);
      builder.append('/');
    }
    
    builder.append(elementName);
    
    return builder.toString();
  }
  
  @Override
  public boolean equals(Object o) {
    if (o == null) return false;
    if (o == this) return true;
    
    if (o.getClass() == getClass()) {
      return compareTo((Event)o) == 0;
    }
    
    return false;
  }
  
  public int hashCode() {
    List<String> thisElements = new ArrayList<String>(context);
    thisElements.add(elementName);
    
    return thisElements.hashCode();
  }

}
