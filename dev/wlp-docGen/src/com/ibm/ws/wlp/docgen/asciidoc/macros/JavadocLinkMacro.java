/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2018
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.wlp.docgen.asciidoc.macros;

import org.asciidoctor.ast.AbstractBlock;
import org.asciidoctor.extension.InlineMacroProcessor;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import org.apache.tools.ant.BuildException;

public class JavadocLinkMacro extends InlineMacroProcessor {

  private Map<String, String> javadocLinks;

  public JavadocLinkMacro(String macroName, File javadocs) {
    super(macroName, new HashMap<String, Object>());
    javadocLinks = loadJavadocLinks(javadocs);
  }

  @Override
  protected String process(AbstractBlock parent, String target, Map<String, Object> attributes) {
    Map<String, Object> options = new HashMap<String, Object>();

    String url = javadocLinks.get(target);
    if (url == null) {
      return target;
    } else {
      options.put("target", url);
      options.put("type", ":link");

      return createInline(parent, "anchor", target, attributes, options).convert();
    }
  }


  private Map<String, String> loadJavadocLinks(File javadocs) {

    Map<String, String> javadocLinks = new HashMap<String, String>();

    if (javadocs != null) {

      File[] dirs = javadocs.listFiles(new FileFilter() {
        @Override
        public boolean accept(File arg0) {
          return arg0.isDirectory();
        }
      });

      if (dirs != null) {
        for (File dir : dirs) {
          File f = new File(dir, "package-list");
          try {
            BufferedReader reader = new BufferedReader(new FileReader(f));
            String line;

            while ((line = reader.readLine()) != null) {
              String packageName = line;
              line = line.replaceAll("\\.", "/");

              String link = javadocs.getName() + "/" + dir.getName() + "/" + line
                  + "/package-summary.html";

              javadocLinks.put(packageName, link);
            }
          } catch (IOException e) {
            throw new BuildException("Unable to read: " + f, e);
          }
        }
      }
    }

    return javadocLinks;
  }
}
