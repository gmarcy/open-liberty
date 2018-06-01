package com.ibm.ws.wlp.docgen.dita.events;

import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import com.ibm.ws.wlp.docgen.dita.DITAHelper;
import com.ibm.ws.wlp.docgen.dita.XMLWriter;

public class SymbolicNameEvent extends SimpleEvent implements Event {

  private String symbolicName;
  
  public SymbolicNameEvent(String symbolicName) {
    this.symbolicName = symbolicName;
  }

  @Override
  public void processEvent(DITAHelper writer) throws XMLStreamException, IOException {
    XMLWriter xml = writer.getWriter();
    xml.pushElement("section");
    xml.pushElement("title");
    xml.pushElement("ph");
    xml.addAttribute("conref", DITAHelper.IDCONTROLLED_ENTITIES_PREFIX + "feat_extend");
    xml.pop("ph"); // ph
    xml.pop("title"); // title
    xml.pushElement("p");
    xml.pushElement("ph");
    xml.addAttribute("conref", DITAHelper.AUTOGEN_ENTITIES_PREFIX + "feat_extend_" + writer.getId());
    xml.pop("ph"); // ph
    xml.pushElement("codeblock");
    xml.addText(symbolicName);
    xml.addText("; type=\"osgi.subsystem.feature\"");
    xml.pop("codeblock"); // codeblock
    xml.pop("p"); // p
    xml.pop("section"); // section
  }
  
  @Override
  public int compareTo(Event o) {
    return symbolicName.compareTo(((SymbolicNameEvent)o).symbolicName);
  }
}
