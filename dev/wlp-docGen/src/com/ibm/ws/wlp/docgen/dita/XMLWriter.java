/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2013
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package com.ibm.ws.wlp.docgen.dita;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 *
 */
public class XMLWriter {
  private static class Indenter {

    private final String LINE_SEPARATOR = getLineSeparator();
    private final XMLStreamWriter xmlWriter;
    private final Writer writer;

    /**
     * @param writer
     * @param w
     */
    public Indenter(XMLStreamWriter xw, Writer w) {
      this.xmlWriter = xw;
      writer = w;
    }

    /**
     * @param i
     * @throws IOException
     * @throws XMLStreamException
     */
    public void indent(int count) throws IOException, XMLStreamException {
      xmlWriter.flush();
      // We are trying to write good looking indented XML. The IBM JDK's
      // XMLStreamWriter
      // will entity encode a \r character on windows so we can't write the line
      // separator
      // on an IBM JDK. On a Sun JDK it doesn't entity encode \r, but if we use
      // the writer to
      // write the line separator the end > of the XML element ends up on the
      // next line.
      // So instead we write a single space to the xmlWriter which causes on all
      // JDKs the
      // element to be closed. We write the line separator using the writer, and
      // the remaining
      // characters using the xml stream writer. Very hacky, but seems to work.
      xmlWriter.writeCharacters(" ");
      writer.write(LINE_SEPARATOR);
      for (int i = 0; i < count; i++) {
        xmlWriter.writeCharacters("  ");
      }

    }

    private String getLineSeparator() {
      String separator = getSystemProperty("line.separator");
      if (separator != null
          && (separator.equals("\n") || separator.equals("\r") || separator
              .equals("\r\n"))) {
        return separator;
      } else {
        return "\n";
      }
    }

    private String getSystemProperty(final String name) {
      return AccessController.doPrivileged(new PrivilegedAction<String>() {
        @Override
        public String run() {
          return System.getProperty(name);
        }
      });
    }

  }

  private final Writer out;
  private final XMLStreamWriter writer;
  private int count = 0;
  private final Indenter indent;
  private final Map<Integer, Boolean> map = new HashMap<Integer, Boolean>();
  private Stack<String> elementStack = new Stack<String>();
  private String fileName;

  public XMLWriter(File f) throws XMLStreamException,
      FactoryConfigurationError, IOException {
    out = new OutputStreamWriter(new FileOutputStream(f), "UTF-8");
    fileName = f.getName();
    writer = XMLOutputFactory.newInstance().createXMLStreamWriter(out);
    indent = new Indenter(writer, out);
    writer.writeStartDocument("UTF-8", "1.0");
    indent.indent(0);
  }

  public void pushElement(String elementName) throws XMLStreamException,
      IOException {
    if (elementStack.contains(elementName) && "section".equals(elementName)) {
      Thread.dumpStack();
    } else {
      elementStack.push(elementName);
    }
    indent.indent(count++);
    map.put(count, false);
    map.put(count - 1, true);
    writer.writeStartElement(elementName);
  }

  public void pop(String ...elements) throws XMLStreamException, IOException {
    String expect = elementStack.peek();
    if (!!!Arrays.asList(elements).contains(expect)) {
      writer.flush();
      System.err.println("expected " + expect + " but was asked to end " + Arrays.toString(elements) + " the stack is " + elementStack + " writing " + fileName);
    }
    elementStack.pop();
    --count;
    if (map.remove(count + 1)) {
      indent.indent(count);
    }
    writer.writeEndElement();
  }

  public void addAttribute(String name, String value) throws XMLStreamException {
    writer.writeAttribute(name, value);
  }

  public void addText(String text) throws XMLStreamException {
    writer.writeCharacters(text);
  }

  /**
   * @param string
   * @throws XMLStreamException
   * @throws IOException
   */
  public void setDTD(String dtd) throws XMLStreamException, IOException {
    indent.indent(count);
    writer.writeDTD(dtd);
  }

  /**
   * @param string
   * @throws XMLStreamException
   * @throws IOException
   */
  public void writeProcessingInstruction(String target)
      throws XMLStreamException, IOException {
    indent.indent(count);
    writer.writeProcessingInstruction(target);
  }

  /**
     * 
     */
  public void close() {
    try {
      writer.writeEndDocument();
      writer.close();
    } catch (XMLStreamException e) {
    }
  }

  public void writeComment(String comment) throws XMLStreamException,
      IOException {
    indent.indent(count);
    writer.writeComment(comment);
  }

  public String peek() {
    return elementStack.peek();
  }
}
