package com.ibm.ws.wlp.docgen.dita.events;

import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import com.ibm.ws.wlp.docgen.dita.DITAHelper;

public abstract class SimpleEvent implements Event {

  @Override
  public void prepareEvents(DITAHelper writer)
      throws XMLStreamException, IOException {

  }

  @Override
  public void postpareEvents(DITAHelper writer)
      throws XMLStreamException, IOException {
  }

  @Override
  public boolean matches(Event e) {
    return (e == null || e.getClass() != this.getClass());
  }
}
