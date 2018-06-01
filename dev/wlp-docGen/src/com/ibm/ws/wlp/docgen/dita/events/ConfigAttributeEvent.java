package com.ibm.ws.wlp.docgen.dita.events;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.xml.stream.XMLStreamException;

import com.ibm.ws.wlp.docgen.dita.Attribute;
import com.ibm.ws.wlp.docgen.dita.DITAHelper;
import com.ibm.ws.wlp.docgen.dita.XMLWriter;

public class ConfigAttributeEvent extends ConfigEvent implements Event {

  private Attribute attrib;

  public ConfigAttributeEvent(List<String> context, Attribute name) {
    super(context, "@" + name.name);
    attrib = name;
  }

  @Override
  public void processEvent(DITAHelper writer)
      throws XMLStreamException, IOException {
    XMLWriter xml = writer.getWriter();
    xml.pushElement("row");
    xml.pushElement("entry");
    xml.addAttribute("colname", "name");
    xml.addText(attrib.name);
    xml.pop("entry"); // entry
    xml.pushElement("entry");
    xml.addAttribute("colname", "type");
    if (attrib.enumValues != null && !!!attrib.enumValues.isEmpty()) {
      xml.pushElement("ul");

      for (String entry : attrib.enumValues.keySet()) {
          xml.pushElement("li");
          xml.addText(entry);
          xml.pop("li"); // li
      }
      xml.pop("ul"); // ul
    } else if (attrib.typeName != null) {
      xml.addText(processType(writer.getTypes(), attrib.typeName, attrib.ref));
      if (attrib.min != null) {
        xml.pushElement("p");
        xml.pushElement("ph");
        xml.addAttribute("conref", DITAHelper.IDCONTROLLED_ENTITIES_PREFIX + "feat_attribute_min");
        xml.pop("ph"); // ph
        xml.addText(attrib.min);
        xml.pop("p"); // p
      }
      
      if (attrib.max != null) {
        xml.pushElement("p");
        xml.pushElement("ph");
        xml.addAttribute("conref", DITAHelper.IDCONTROLLED_ENTITIES_PREFIX + "feat_attribute_max");
        xml.pop("ph"); // ph
        xml.addText(attrib.max);
        xml.pop("p"); // p
      }
    } else {
      System.out.println("No type or enum data");
    }
    xml.pop("entry"); // entry
    xml.pushElement("entry");
    xml.addAttribute("colname", "default");
    xml.addText(attrib.defaultValue);
    xml.pop("entry"); // entry
    xml.pushElement("entry");
    xml.addAttribute("colname", "description");
    xml.addText(attrib.desc);

    if (attrib.enumValues != null && !!!attrib.enumValues.isEmpty()) {
      xml.pushElement("dl");
      for (Map.Entry<String, String> entry : attrib.enumValues.entrySet()) {
          xml.pushElement("dlentry");
          xml.pushElement("dt");
          xml.addText(entry.getKey());
          xml.pop("dt"); // dt
          xml.pushElement("dd");
          xml.addText(entry.getValue());
          xml.pop("dd"); // dd
          xml.pop("dlentry"); // dlentry
      }

      xml.pop("dl"); // dl
    }
    xml.pop("entry"); // entry
    xml.pop("row"); // row
  }

  @Override
  public void prepareEvents(DITAHelper writer)
      throws XMLStreamException, IOException {
    XMLWriter xml = writer.getWriter();
    xml.pushElement("table");
    xml.pushElement("tgroup");
    xml.addAttribute("cols", "4");
    xml.pushElement("thead");
    xml.pushElement("row");
    xml.addAttribute("valign", "bottom");
    xml.pushElement("entry");
    xml.addAttribute("colname", "name");
    xml.pushElement("ph");
    xml.addAttribute("conref", DITAHelper.IDCONTROLLED_ENTITIES_PREFIX + "feat_colhead_attribute_name");
    xml.pop("ph"); // ph
    xml.pop("entry"); // entry
    xml.pushElement("entry");
    xml.addAttribute("colname", "type");
    xml.pushElement("ph");
    xml.addAttribute("conref", DITAHelper.IDCONTROLLED_ENTITIES_PREFIX + "feat_colhead_data_type");
    xml.pop("ph"); // ph
    xml.pop("entry"); // entry
    xml.pushElement("entry");
    xml.addAttribute("colname", "default");
    xml.pushElement("ph");
    xml.addAttribute("conref", DITAHelper.IDCONTROLLED_ENTITIES_PREFIX + "feat_colhead_default_value");
    xml.pop("ph"); // ph
    xml.pop("entry"); // entry
    xml.pushElement("entry");
    xml.addAttribute("colname", "description");
    xml.pushElement("ph");
    xml.addAttribute("conref", DITAHelper.IDCONTROLLED_ENTITIES_PREFIX + "feat_colhead_description");
    xml.pop("ph"); // ph
    xml.pop("entry"); // entry
    xml.pop("row"); // row
    xml.pop("thead"); // thead
    xml.pushElement("tbody");
  }

  @Override
  public void postpareEvents(DITAHelper writer)
      throws XMLStreamException, IOException {
    XMLWriter xml = writer.getWriter();
    xml.pop("tbody"); // tbody
    xml.pop("tgroup"); // tgroup
    xml.pop("table"); // table
  }

  /**
   * @param types 
   * @param type
   * @return
   */
  private String processType(ResourceBundle types, String type, String insert) {
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

}
