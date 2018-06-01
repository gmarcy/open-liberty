package com.ibm.ws.wlp.docgen.dita.events;

import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import com.ibm.ws.wlp.docgen.dita.DITAHelper;
import com.ibm.ws.wlp.docgen.dita.XMLWriter;

public abstract class TextNodeEvent extends SimpleEvent {

  private String elementName;
  private String elementValue;
  
  public TextNodeEvent(String name, String value) {
    elementName = name;
    elementValue = value;
  }
  
  @Override
  public void processEvent(DITAHelper writer)
      throws XMLStreamException, IOException {
    XMLWriter xml = writer.getWriter();
    xml.pushElement(elementName);
    xml.addText(elementValue);
    xml.pop(elementName);
  }

  @Override
  public int compareTo(Event o) {
    return elementValue.compareTo(((TextNodeEvent)o).elementValue);
  }
}
