package com.ibm.ws.wlp.docgen.dita.events;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Stack;

import javax.xml.stream.XMLStreamException;

import com.ibm.ws.wlp.docgen.dita.DITAHelper;
import com.ibm.ws.wlp.docgen.dita.XMLWriter;

public class ConfigChildElementEvent extends ConfigEvent implements Event {

  private final String type;
  private final String description;
  private final boolean required;
  
  public ConfigChildElementEvent(Stack<String> config, String name,
      String type, String description, boolean required) {
    super(config, name);
    this.type = type;
    this.description = description;
    this.required = required;
  }

  @Override
  public void processEvent(DITAHelper writer)
      throws XMLStreamException, IOException {
    XMLWriter xml = writer.getWriter();
    xml.pushElement("plentry");
    xml.addAttribute("id", generateId(context, elementName));
    xml.pushElement("pt");
    xml.addText(toBreadcrumb(context, elementName));
    xml.pop("pt"); // pt
    xml.pushElement("pd");
    xml.addAttribute("outputclass", "desc");
    xml.addText(description);
    xml.pop("pd"); // pd
    xml.pushElement("pd");
    xml.addAttribute("outputclass", "required");
    xml.addText(String.valueOf(required));
    xml.pop("pd"); // pd
    xml.pushElement("pd");
    xml.addAttribute("outputclass", "type");
    if (type != null) {
        xml.addText(processType(writer.getTypes(), type, ""));
    }
  }

  public static String generateId(List<String> context, String elementName) {
    StringBuilder builder = new StringBuilder();

    if (context != null) {
      for (String element : context) {
        builder.append(element);
        builder.append("__");
      }
    }

    builder.append(elementName);

    return builder.toString();
  }

  public static String toBreadcrumb(List<String> context, String elementName) {
    StringBuilder builder = new StringBuilder();
    
    for (String element : context) {
      builder.append(element);
      builder.append(" > ");
    }

    builder.append(elementName);

    return builder.toString();
  }

  @Override
  public void prepareEvents(DITAHelper writer)
      throws XMLStreamException, IOException {
      XMLWriter xml = writer.getWriter();
      xml.pushElement("parml");
      xml.addAttribute("outputclass", "childelements");
  }

  /**
   * @param types 
   * @param type
   * @return
   */
  public static String processType(ResourceBundle types, String type, String insert) {
      String resolvedType = type;

      try {
          resolvedType = types.getString(type);
      } catch (MissingResourceException mre) {
          // do nothing here
      }

      if (resolvedType.contains("{")) {
          resolvedType = MessageFormat.format(resolvedType, insert);
      }

      return resolvedType;
  }

  @Override
  public boolean matches(Event e) {
    if (super.matches(e)) {
      return true;
    }
    
    if (e instanceof ConfigChildElementEvent) {
      ConfigChildElementEvent event = (ConfigChildElementEvent) e;
      if (context.size() != event.context.size()) {
        return true;
      }
    }
    
    return false;
  }
}
