package com.ibm.ws.wlp.docgen.dita.events;

import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import com.ibm.ws.wlp.docgen.dita.DITAHelper;

public class DescriptionEvent extends TextNodeEvent implements Event {
  private boolean needSection = false;

  public DescriptionEvent(String value) {
    super("shortdesc", value);
  }

  public DescriptionEvent(String value, boolean needSection) {
    this(value);
    this.needSection = needSection;
  }
  
  @Override
  public void postpareEvents(DITAHelper writer)
      throws XMLStreamException, IOException {
    writer.getWriter().pushElement("refbody");
    if (needSection) {
      writer.getWriter().pushElement("section");
    }
  }
}
