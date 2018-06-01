package com.ibm.ws.wlp.docgen.dita.events;

import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import com.ibm.ws.wlp.docgen.dita.DITAHelper;
import com.ibm.ws.wlp.docgen.dita.XMLWriter;

public abstract class PackageEvent extends SimpleEvent {

  private String type;
  private String packageName;
  private String javadocLink;
  
  public PackageEvent(String type, String packageName, String link) {
    this.type = type;
    this.packageName = packageName;
    this.javadocLink = link;
  }
  
  @Override
  public void processEvent(DITAHelper writer)
      throws XMLStreamException, IOException {
    XMLWriter xml = writer.getWriter();
    xml.pushElement("li");
    if (javadocLink != null) {
      xml.pushElement("xref");
      xml.addAttribute("format", "html");
      xml.addAttribute("href", "../../" + javadocLink);
      xml.addAttribute("scope", "peer");
    }
    xml.addText(packageName);
    if (javadocLink != null) {
      xml.pop("xref"); // xref
    }
    xml.pop("li"); // li
  }

  @Override
  public void prepareEvents(DITAHelper writer)
      throws XMLStreamException, IOException {
    XMLWriter xml = writer.getWriter();
    xml.pushElement("section");
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
    return packageName.compareTo(((PackageEvent)o).packageName);
  }
}
