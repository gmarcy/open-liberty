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
import java.util.Map;
import java.util.HashMap;

public class ConfigFeatureLinkMacro extends InlineMacroProcessor {

  public ConfigFeatureLinkMacro(String macroName) {
    super(macroName, new HashMap<String, Object>());
  }

  @Override
  protected String process(AbstractBlock parent, String target, Map<String, Object> attributes) {
    Map<String, Object> options = new HashMap<String, Object>();
    options.put("target", "rwlp_" + name + "_" + target + ".html");
    options.put("type", ":link");

    return createInline(parent, "anchor", target, attributes, options).convert();
  }
}
