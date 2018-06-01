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
package com.ibm.ws.wlp.docgen.asciidoc;

import java.io.PrintStream;
import java.util.Map;

public interface Formatter {

  public class Factory {
    public static Formatter create(String format, Map<String, String> attributes, PrintStream out) {
      if ("adoc".equals(format) || "asciidoc".equals(format)) {
        return new AdocFormatter(out, attributes);
      }
      throw new IllegalArgumentException(format + " is not known");
    }
  }

  public void debug();

  // Table stuff
  public Formatter table(int colCount);
  public Formatter tableHeader();
  public Formatter tableRow();
  public Formatter tableCell();
  public Formatter tableCell(int colSpan);

  // Bold
  public Formatter bold();

  public Formatter code(String lang);

  // header
  public Formatter header(int level);

  // bullets
  public Formatter bullets();
  public Formatter bullet();

  // Controlly stuff
  public Formatter end();
  // Outputs a crlf. Might be able to remove in favour of end.
  public Formatter next();

  // Texty stuff
  public Formatter nl();
  public Formatter text(String text);
  public Formatter raw(String text);
  public Formatter link(String text, String url);

  public Formatter reference(String text, String id);

  public Formatter referable(String id);
}
