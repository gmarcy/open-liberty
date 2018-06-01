package com.ibm.ws.wlp.docgen.dita;

import java.io.IOException;

public interface FeatureVisitor extends Visitor {
  public void visitSymbolicName(String symbolicName) throws IOException;
  public void visitSuperceededBy(String feature) throws IOException;
  public void visitEnables(String feature) throws IOException;
  public void visitEnabledBy(String feature) throws IOException;
  public void visitAPIPackage(String packageName, String type, String javadocLink2) throws IOException;
  public void visitSPIPackage(String packageName, String javadocLink) throws IOException;
  public abstract void visitKernel();
  public void visitJavaLevels(String javaLevels);
}
