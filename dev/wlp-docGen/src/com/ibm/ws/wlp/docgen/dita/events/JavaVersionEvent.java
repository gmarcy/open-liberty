package com.ibm.ws.wlp.docgen.dita.events;

import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import com.ibm.ws.wlp.docgen.dita.DITAHelper;
import com.ibm.ws.wlp.docgen.dita.XMLWriter;

public class JavaVersionEvent extends SimpleEvent implements Event {

  private String javaVersion;
  
  public JavaVersionEvent(String javaLevel) {
    javaVersion = javaLevel;
  }

  @Override
  public void processEvent(DITAHelper writer)
      throws XMLStreamException, IOException {
    XMLWriter xml = writer.getWriter();
    xml.pushElement("li");
    xml.addText(javaVersion);
    xml.pop("li"); // li
  }

  @Override
  public void prepareEvents(DITAHelper writer)
      throws XMLStreamException, IOException {
    XMLWriter xml = writer.getWriter();
    xml.pushElement("section");
    xml.pushElement("title");
    xml.pushElement("ph");
    xml.addAttribute("conref", DITAHelper.IDCONTROLLED_ENTITIES_PREFIX + "feat_supported_java_versions");
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
    return javaVersion.compareTo(((JavaVersionEvent)o).javaVersion);
  }
}
