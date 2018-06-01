package com.ibm.ws.wlp.docgen.dita.events;

import java.io.IOException;
import java.util.Stack;

import javax.xml.stream.XMLStreamException;

import com.ibm.ws.wlp.docgen.dita.DITAHelper;
import com.ibm.ws.wlp.docgen.dita.XMLWriter;

public class ConfigChildElementEndEvent extends ConfigEvent implements Event {
  private String type;

  public ConfigChildElementEndEvent(Stack<String> config, String name, String type) {
    super(config, "~");
    this.type = type;
  }

  @Override
  public void processEvent(DITAHelper writer)
      throws XMLStreamException, IOException {
    XMLWriter xml = writer.getWriter();
    xml.pop("pd"); // pd
    xml.pop("plentry"); // plentry
    if (context.size() >= 1) {
      xml.pop("parml"); // parml
    }
  }
}
