package com.ibm.ws.wlp.docgen.dita.events;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import com.ibm.ws.wlp.docgen.dita.DITAHelper;
import com.ibm.ws.wlp.docgen.dita.XMLWriter;

public class ConfigTOCEvent extends ConfigEvent implements Event {
  private static boolean first = true;
  private static int lastContextSize = 0;
  private static boolean writeFeatureToc = true;
  
  public ConfigTOCEvent(String name) {
    this(new ArrayList<String>(), name);
  }

  public ConfigTOCEvent(List<String> context, String name) {
    super(context, name);
  }

  @Override
  public void processEvent(DITAHelper writer)
      throws XMLStreamException, IOException {
    XMLWriter xml = writer.getWriter();
    
    // if the context size has increased then we need to do a nested ul
    if (lastContextSize < context.size()) {
    // if the context size has reduced then we need to do close a ul
      for (int i = lastContextSize; i != context.size(); i++) {
        xml.pushElement("ul");
      }
    } else if (lastContextSize > context.size()) {
      for (int i = lastContextSize; i != context.size(); i--) {
        xml.pop("li"); // li
        xml.pop("ul"); // ul
      }
    } 
    
    if (!!!first && lastContextSize >= context.size()){
      xml.pop("li"); // li
    }
    
    xml.pushElement("li");
    xml.pushElement("xref");
    xml.addAttribute("href", getHref(writer.getId()));
    xml.addAttribute("format", "dita");
    xml.addText(elementName);
    xml.pop("xref"); // xref
    
    // store the context size
    lastContextSize = context.size();
    first = false;
  }

  private String getHref(String featureName) {
    StringBuilder builder = new StringBuilder();
    if (writeFeatureToc) {
      builder.append("rwlp_config_");
      String name = elementName;
      if (context != null && context.size() > 0) {
        name = context.get(0);
      }
      builder.append(name);
      builder.append(".dita");
    }
    if ((context != null && context.size() > 0) || !!!writeFeatureToc) {
      builder.append("#rwlp_config_");
      builder.append(featureName);
      builder.append("/");
      for (String element : context) {
        builder.append(element);
        builder.append("__");
      }
      builder.append(elementName);
    }
    
    return builder.toString();
  }

  @Override
  public void prepareEvents(DITAHelper writer)
      throws XMLStreamException, IOException {
    XMLWriter xml = writer.getWriter();
    
    // Write the start of the config section.
    // TODO we should move this out of here
    if (writeFeatureToc ) {
      xml.pushElement("section");
      xml.pushElement("title"); 
      xml.pushElement("ph");
      xml.addAttribute("conref", DITAHelper.IDCONTROLLED_ENTITIES_PREFIX + "feat_config");
      xml.pop("ph"); // ph
      xml.pop("title"); // title
      xml.writeProcessingInstruction("Pub Caret 16");
      xml.pushElement("p");
      xml.pushElement("ph");
      
      String featureId = writer.getId();
      String prefix = "";
      if ("javaeeClient-7.0".equals(featureId)) {
        prefix = "client_";
      }
      xml.addAttribute("conref", DITAHelper.AUTOGEN_ENTITIES_PREFIX + "feat_config_" + prefix + writer.getId());
      xml.pop("ph"); // ph
      xml.pop("p"); // p
    }
    
    // TOC is in an unordered list.
    xml.pushElement("ul");
  }

  @Override
  public void postpareEvents(DITAHelper writer)
      throws XMLStreamException, IOException {
    XMLWriter xml = writer.getWriter();
    for (; lastContextSize != 0; lastContextSize--) {
      xml.pop("li"); // li
      xml.pop("ul"); // ul
    }
    xml.pop("li"); // li
    xml.writeProcessingInstruction("Pub Caret 15");
    xml.pop("ul"); // ul

    
//    xml.pushElement("dl"); // TODO this should not be here, but it is easier to have it here for now.
    first = true;
  }
  
  public static void setWriteFeatureToc(boolean write) {
    writeFeatureToc = write;
  }
}
