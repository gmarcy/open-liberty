package com.ibm.ws.wlp.docgen.dita.events;

import java.io.IOException;
import java.util.ArrayList;

import javax.xml.stream.XMLStreamException;

import com.ibm.ws.wlp.docgen.dita.DITAHelper;
import com.ibm.ws.wlp.docgen.dita.XMLWriter;

public class ConfigElementEvent extends ConfigEvent {

  private String desc;

  public ConfigElementEvent(String name, String desc) {
    super(new ArrayList<String>(), name);
    this.desc = desc;
  }

  @Override
  public void processEvent(DITAHelper writer)
      throws XMLStreamException, IOException {
    XMLWriter xml = writer.getWriter();
    xml.pushElement("dlentry");
    xml.addAttribute("id", elementName);
    xml.pushElement("dt");
    xml.pushElement("ph");
    xml.addText(elementName);
    xml.pop("ph"); // ph
    xml.pop("dt"); // dt
    xml.pushElement("dd");
    xml.addText(desc);
  }

  @Override
  public void prepareEvents(DITAHelper writer)
      throws XMLStreamException, IOException {
  }

  @Override
  public void postpareEvents(DITAHelper writer)
      throws XMLStreamException, IOException {
  }
}
