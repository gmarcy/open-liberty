package com.ibm.ws.wlp.docgen.dita.events;

import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import com.ibm.ws.wlp.docgen.dita.DITAHelper;
import com.ibm.ws.wlp.docgen.dita.XMLWriter;

public class FeatureEnablementEvent extends SimpleEvent implements Event {

  @Override
  public void processEvent(DITAHelper writer) throws XMLStreamException, IOException {
    XMLWriter xml = writer.getWriter();
    xml.pushElement("section");
    xml.pushElement("title");
    xml.pushElement("ph");
    xml.addAttribute("conref", DITAHelper.IDCONTROLLED_ENTITIES_PREFIX + "feat_enable");
    xml.pop("ph"); // ph
    xml.pop("title"); // title
    xml.pushElement("p");
    xml.pushElement("ph");
    String featureId = writer.getId();
    String prefix = "";
    if ("javaeeClient-7.0".equals(featureId)) {
      prefix = "client_";
    }
    xml.addAttribute("conref", DITAHelper.AUTOGEN_ENTITIES_PREFIX + "feat_enable_" + prefix + featureId);
    xml.pop("ph"); // ph
    xml.pushElement("codeblock");
    xml.addText("<feature>");
    xml.addText(writer.getId());
    xml.addText("</feature>");
    xml.pop("codeblock"); // codeblock
    xml.pop("p"); // p
    xml.pop("section"); // section
  }

  @Override
  public int compareTo(Event o) {
    return 0;
  }
}
