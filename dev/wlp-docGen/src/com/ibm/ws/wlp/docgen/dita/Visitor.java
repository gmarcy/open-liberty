package com.ibm.ws.wlp.docgen.dita;

import java.io.IOException;

public interface Visitor {

  public void visitName(String name) throws IOException;

  public void visitDescription(String description) throws IOException;

  public void visitConfigElement(String name, String desc, String type) throws IOException;

  public void visitConfigAttribute(Attribute attribute) throws IOException;

  public void visitChildElement(String name, String type, String description, boolean required) throws IOException;

  public void done() throws IOException;

  public void visitTitle(String displayName) throws IOException;

  public void endChildElement() throws IOException;

}
