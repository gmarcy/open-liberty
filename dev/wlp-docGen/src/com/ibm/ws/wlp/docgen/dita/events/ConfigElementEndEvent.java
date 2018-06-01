package com.ibm.ws.wlp.docgen.dita.events;

import java.io.IOException;
import java.util.Arrays;

import javax.xml.stream.XMLStreamException;

import com.ibm.ws.wlp.docgen.dita.DITAHelper;
import com.ibm.ws.wlp.docgen.dita.XMLWriter;

public class ConfigElementEndEvent extends ConfigEvent implements Event {

  public ConfigElementEndEvent(String name) {
    super(Arrays.asList(name), "~");
  }

  @Override
  public void processEvent(DITAHelper writer)
      throws XMLStreamException, IOException {
    XMLWriter xml = writer.getWriter();
    xml.pop("dd"); // dd
    xml.pop("dlentry"); // dlentry
  }

}
