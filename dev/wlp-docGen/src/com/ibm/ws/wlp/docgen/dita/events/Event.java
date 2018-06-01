package com.ibm.ws.wlp.docgen.dita.events;

import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import com.ibm.ws.wlp.docgen.dita.DITAHelper;

public interface Event extends Comparable<Event> {
  public void processEvent(DITAHelper writer)  throws XMLStreamException, IOException;
  public void prepareEvents(DITAHelper writer) throws XMLStreamException, IOException;
  public void postpareEvents(DITAHelper writer) throws XMLStreamException, IOException;
  public boolean matches(Event e);
}
