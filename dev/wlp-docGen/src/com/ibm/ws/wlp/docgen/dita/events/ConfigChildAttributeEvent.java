package com.ibm.ws.wlp.docgen.dita.events;

import java.io.IOException;
import java.util.Stack;

import javax.xml.stream.XMLStreamException;

import com.ibm.ws.wlp.docgen.dita.DITAHelper;
import com.ibm.ws.wlp.docgen.dita.XMLWriter;

public class ConfigChildAttributeEvent extends ConfigEvent implements Event {
  private String type;
  private String desc;
  private boolean required;
  public ConfigChildAttributeEvent(Stack<String> config, String name,
      String type, String description, boolean required) {
    super(config, name);
    this.type = type;
    this.desc = description;
    this.required = required;
  }

  @Override
  public void processEvent(DITAHelper writer) throws XMLStreamException,
      IOException {
    XMLWriter xml = writer.getWriter();
    xml.pushElement("parml");
    xml.pushElement("plentry");
    xml.addAttribute("id", ConfigChildElementEvent.generateId(context, elementName));
    xml.pushElement("pt");
    xml.addText(ConfigChildElementEvent.toBreadcrumb(context, elementName));
    xml.pop("pt");
    xml.pushElement("pd");
    xml.addAttribute("outputclass", "desc");
    xml.addText(desc);
    xml.pop("pd");
    xml.pushElement("pd");
    xml.addAttribute("outputclass", "required");
    xml.addText(String.valueOf(required));
    xml.pop("pd");
    xml.pushElement("pd");
    xml.addAttribute("outputclass", "type");
    if (type != null) {
      xml.addText(ConfigChildElementEvent.processType(writer.getTypes(), type, ""));
    }
    xml.pop("pd");
    xml.pop("plentry");
    xml.pop("parml");
  }

}
