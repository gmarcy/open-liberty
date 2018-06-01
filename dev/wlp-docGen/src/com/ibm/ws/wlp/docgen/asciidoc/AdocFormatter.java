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
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class AdocFormatter implements Formatter {

  private Formatter current;
  private Formatter root;

  /** Pattern that checks for numeric HTML encoding. */
  private static final Pattern ENCODING_PATTERN = Pattern.compile("&\\#\\d{2,3};.*");

  public AdocFormatter(PrintStream o, Map<String, String> attribs) {
    current = new RootFormatter(o, attribs, this);
    root = current;
  }

  private Formatter setCurrent(Formatter f) {
    if (f == this) {
      current = root;
    } else {
      current = f;
    }
    return this;
  }
  // Table stuff
  public Formatter table(int cols) {
    return setCurrent(current.table(cols));
  }
  public Formatter tableHeader() {
    return setCurrent(current.tableHeader());
  }
  public Formatter tableRow() {
    return setCurrent(current.tableRow());
  }
  public Formatter tableCell(int colSpan) {
    return setCurrent(current.tableCell(colSpan));
  }
  public Formatter tableCell() {
    return setCurrent(current.tableCell());
  }

  // Bold
  public Formatter bold() {
    return setCurrent(current.bold());
  }

  public Formatter code(String lang) {
    return setCurrent(current.code(lang));
  }

  // header
  public Formatter header(int level) {
    return setCurrent(current.header(level));
  }

  // bullets
  public Formatter bullets() {
    return setCurrent(current.bullets());
  }
  public Formatter bullet() {
    return setCurrent(current.bullet());
  }

  // Controlly stuff
  public Formatter end() {
    return setCurrent(current.end());
  }
  // Outputs a crlf. Might be able to remove in favour of end.
  public Formatter next() {
    setCurrent(current.next());
    return this;
  }

  // Texty stuff
  public Formatter nl() {
    current.nl();
    return this;
  }
  public Formatter text(String text) {
    current.text(text);
    return this;
  }
  public Formatter raw(String text) {
    current.raw(text);
    return this;
  }
  public Formatter link(String text, String url) {
    current.link(text, url);
    return this;
  }

  public Formatter reference(String text, String id) {
    current.reference(text, id);
    return this;
  }

  public Formatter referable(String id) {
    current.referable(id);
    return this;
  }

  public void debug() {
    System.out.println(current);
  }

  /**
   * Encode a string for markdown. This method backslash-escapes characters
   * where possible to keep the markdown as readable as possible. This method assumes that
   * the input string contains no (intentional) markdown syntax and will escape any
   * markdown syntax found treating them as literal characters.
   *
   * @param input The input string to encode.
   * @return The string with relevant characters encoded for markdown.
   */
  private static String encodeForMarkdown(String input) {
    StringBuilder sb = new StringBuilder();
    for (int idx = 0; idx < input.length(); idx++) {

      /*
	   * Check for characters encoded with HTML number encoding to ensure
       * we don't replace the '#'. This is probably unlikely, but valid.
       */
      String ss = input.substring(idx);
      if (ENCODING_PATTERN.matcher(ss).matches()) {
        int end = ss.indexOf(';');
        sb.append(ss.substring(0, end + 1));
        idx += end;
        continue;
      }

      /*
       * Determine whether we need to encode or escape the character. Usually, we can use
       * backslash-escaping as that is the accepted standard in markdown. However;
       * that is not valid for the pipe character ('|') so we use HTTP numeric encoding.
       */
      char c = input.charAt(idx);
      if (c == '|') {
        sb.append("&#124;");
      } else if (c =='#' || c== '!' || c == '(' || c == ')' || c == '*' || c == '+' || c == '-' || c == '.'
    		  || c == '[' || c == '\\' || c == ']' || c == '`' || c == '_' || c == '{' || c == '}') {
        sb.append('\\').append(c);
      } else {
        sb.append(c);
      }
    }
    return sb.toString();
  }

  private static class CodeFormatter extends TextFormatter {
    public CodeFormatter(PrintStream out, String lang, Formatter parent) {
      super(out, "code", parent);
      out.println();
      if (lang != null && lang.length() == 0) {
        out.println("[source," + lang + "]");
      }
      out.println("----");
    }
    // Controlly stuff
    public Formatter end() {
      out.println();
      out.println("----");
      return super.end();
    }

    // Texty stuff
    public Formatter nl() {
      next();
      return this;
    }
    public Formatter text(String text) {
      raw(text);
      return this;
    }
    public Formatter link(String text, String url) {
      throw new IllegalStateException("You can't add a link in a code block");
    }
  }

  private static class Table extends BaseFormatter {
    public Table(PrintStream out, int cols, Formatter parent) {
      super(out, "table", parent);

      if (cols > 0) {
        StringBuilder colSpec = new StringBuilder();
        colSpec.append("[cols=\"");
        for (int i = 0; i < cols; i++) {
          colSpec.append('a');
          if (i + 1 < cols) {
            colSpec.append(',');
          }
        }
        colSpec.append("\",width=\"100%\"]");
        out.println(colSpec);
      }

      out.println("|===");
    }

    public Formatter end() {
      out.println();
      out.println("|===");
      return super.end();
    }
    public Formatter tableHeader() {
      return new TableHeader(out, this);
    }
    public Formatter tableRow() {
      return new TableRow(out, this);
    }
    public Formatter tableCell(int colSpan) {
      return tableRow().tableCell(colSpan);
    }
    public Formatter tableCell() {
      return tableRow().tableCell();
    }
  }

  private static class TableHeader extends TableRow {
    public TableHeader(PrintStream out, Formatter parent) {
      super(out, parent);
      first = false;
    }

    public Formatter tableCell() {
      return new HeaderCell(out, this);
    }
  }

  private static class HeaderCell extends TextFormatter {
    public HeaderCell(PrintStream out, Formatter parent) {
      super(out, "cell", parent);
      out.print("|");
    }
    public Formatter tableCell() {
      return end().tableCell();
    }

    public Formatter tableRow() {
      return super.end().tableRow();
    }
    public Formatter tableCell(int colSpan) {
      return super.end().tableCell(colSpan);
    }
  }

  private static class TableRow extends BaseFormatter {
    protected boolean first = true;
    public TableRow(PrintStream out, Formatter parent) {
      super(out, "row", parent);
    }

    public Formatter tableCell(int colSpan) {
      if (first) {
        out.println();
        first = false;
      }
      return new TableCell(out, colSpan, this);
    }

    public Formatter tableRow() {
      return end().tableRow();
    }

    public Formatter tableCell() {
      return end().tableCell(1);
    }

    public Formatter end() {
      return super.end();
    }
  }

  private static class TableCell extends TextFormatter {
    public TableCell(PrintStream out, int colSpan, Formatter parent) {
      super(out, "cell", parent);
      out.println();
      if (colSpan > 1) {
        out.print(colSpan);
        out.print('+');
      }
      out.print("|");
    }

    public Formatter bullets() {
      return new BulletsFormatter(out, this, 1);
    }

    public Formatter bullet() {
      return bullets().bullet();
    }

    public Formatter end() {
      return super.end();
    }

    public Formatter bold() {
      return new BoldFormatter(out, this);
    }

    public Formatter tableCell() {
      return end().tableCell();
    }

    public Formatter tableRow() {
      return super.end().tableRow();
    }

    public Formatter tableCell(int colSpan) {
      return end().tableCell(colSpan);
    }

    public Formatter next() {
      Formatter f = parent;
      // if we call next end the table
      while (!(f instanceof RootFormatter)) {
        f = f.end();
      }
      // this is a really dodgy hack to reset the parent back to the root.
      // (MdFormatter)((RootFormatter)f).parent).current = f;
      return f;
    }
  }

  private static class BulletsFormatter extends BaseFormatter {
    private int level;
    public BulletsFormatter(PrintStream out, Formatter parent, int level) {
      super(out, "bullets", parent);
      this.level = level;
    }

    public Formatter bullets() {
      return new BulletsFormatter(out, this, level + 1);
    }

    public Formatter bullet() {
      return new BulletFormatter(out, this, level);
    }
  }

  private static class BulletFormatter extends TextFormatter {
    private int level;
    public BulletFormatter(PrintStream out, Formatter parent, int level) {
      super(out, "bullet", parent);
      this.level = level;
      for (int i = 0; i < level; i++) {
        out.print("*");
      }
      out.print(" ");
    }

    public Formatter bullets()  {
      return new BulletsFormatter(out, this, level + 1);
    }

    public Formatter bullet() {
      return new BulletFormatter(out, end(), level);
    }

    public Formatter end() {
      next();
      return super.end();
    }
  }

  private static class HeaderFormatter extends TextFormatter {
    private int level;
    private Map<String, String> attributes;
    public HeaderFormatter(PrintStream out, Formatter parent, int level) {
      super(out, "header", parent);
      for (int i = 0; i < level; i++) {
        out.print("=");
      }
      out.print(" ");
      this.level = level;
    }

    public Formatter end() {
      out.println();
      if (level == 1 && attributes != null) {
        for (Map.Entry<String, String> attrib : attributes.entrySet()) {
          out.print(':');
          out.print(attrib.getKey());
          out.print(": ");
          out.println(attrib.getValue());
        }
      }
      return super.end();
    }

    public Formatter next() {
      return end();
    }

    public void setAttributes(Map<String, String> attribs) {
      attributes = attribs;
    }
  }

  private static class BoldFormatter extends TextFormatter {
    public BoldFormatter(PrintStream out, Formatter parent) {
      super(out, "bold", parent);
      raw("*");
    }

    public Formatter end() {
      raw("*");
      return super.end();
    }
  }

  private static class TextFormatter extends BaseFormatter {
    public TextFormatter(PrintStream out, String type, Formatter parent) {
      super(out, type, parent);
    }
    @Override
    public Formatter text(String text) {
      out.print(text);
      return this;
    }

    @Override
    public Formatter raw(String raw) {
      out.print(raw);
      return this;
    }

    @Override
    public Formatter link(String text, String url) {
      String formattedLink;

      if ("javadoc".equals(url) || "feature".equals(url) || "config".equals(url)) {
        formattedLink = url + ":" + text;
      } else if (url.indexOf(':') == -1) {
          formattedLink = "link:" + url;
      } else {
        formattedLink = url + '[' + text + ']';
      }
      out.print(formattedLink);
      out.print("[]");
      return this;
    }

    @Override
    public Formatter reference(String text, String id) {
      out.print("<<");
      out.print(id.replaceAll("\\__", "/"));
      out.print(',');
      out.print(text);
      out.print(">>");
      return this;
    }

    @Override
    public Formatter referable(String id) {
      out.print("[[");
      out.print(id.replaceAll("\\__", "/"));
      out.print("]]");
      return this;
    }

    @Override
    public Formatter next() {
      out.println();
      return this;
    }

    @Override
    public Formatter nl() {
      out.println(" +");
      return this;
    }
  }

  private static class RootFormatter extends TextFormatter {
    private Map<String, String> attributes;
    public RootFormatter(PrintStream o, Map<String, String> attribs, Formatter p) {
      super(o, "root", p);
      attributes = attribs;
    }
    // Table stuff
    public Formatter table(int cols) {
      return new Table(out, cols, this);
    }
    public Formatter tableHeader() {
      return table(-1).tableHeader();
    }
    public Formatter tableRow() {
      return table(-1).tableRow();
    }
    public Formatter tableCell(int colSpan) {
      return table(-1).tableRow().tableCell(colSpan);
    }
    public Formatter tableCell() {
      return table(-1).tableRow().tableCell();
    }

    // Bold
    public Formatter bold() {
      return new BoldFormatter(out, this);
    }

    public Formatter code(String lang) {
      return new CodeFormatter(out, lang, this);
    }

    // header
    public Formatter header(int level) {
      HeaderFormatter header = new HeaderFormatter(out, this, level);
      if (level == 1 && attributes != null) {
        header.setAttributes(attributes);
      }
      return header;
    }

    // bullets
    public Formatter bullets() {
      return new BulletsFormatter(out, this, 1);
    }
    public Formatter bullet() {
      return bullets().bullet();
    }
  }

  private static class BaseFormatter implements Formatter {
    protected Formatter parent;
    protected PrintStream out;
    private String sectionType;
    public BaseFormatter(PrintStream o, String st, Formatter p) {
      out = o;
      parent = p;
      sectionType = st;
    }

    public Formatter table(int cols) { throw new IllegalStateException("Can't create a table in a " + sectionType + " section"); }
    public Formatter tableHeader() { throw new IllegalStateException("Can't create a table header in a " + sectionType + " section"); }
    public Formatter tableRow() { throw new IllegalStateException("Can't create a table row in a " + sectionType + " section"); }
    public Formatter tableCell(int colSpan) { throw new IllegalStateException("Can't create a table cell in a " + sectionType + " section"); }
    public Formatter tableCell()  { throw new IllegalStateException("Can't create a table cell in a " + sectionType + " section"); }
    public Formatter header(int level)  { throw new IllegalStateException("Can't create a header in a " + sectionType + " section"); }
    public Formatter bullets()  { throw new IllegalStateException("Can't create bullets in a " + sectionType + " section"); }
    public Formatter bullet() { throw new IllegalStateException("Can't create a bullet in a " + sectionType + " section"); }
    public Formatter next() { throw new IllegalStateException("Can't go next in a " + sectionType + " section"); }
    public Formatter bold() { throw new IllegalStateException("Can't create bold style in a " + sectionType + " section"); }
    public Formatter code(String lang) { throw new IllegalStateException("Can't create code style in a " + sectionType + " section"); }

    // Texty stuff
    public Formatter nl() { throw new IllegalStateException("Can't create new line in a " + sectionType + " section"); }
    public Formatter text(String text) { throw new IllegalStateException("Can't create a text in a " + sectionType + " section"); }
    public Formatter raw(String text) { throw new IllegalStateException("Can't create a text in a " + sectionType + " section"); }
    public Formatter link(String text, String url) { throw new IllegalStateException("Can't create a link in a " + sectionType + " section"); }
    public Formatter reference(String text, String id) { throw new IllegalStateException("Can't create a link in a " + sectionType + " section"); }
    public Formatter referable(String id) { throw new IllegalStateException("Can't create an anchor in a " + sectionType + " section"); }
    public Formatter end() {
      // System.out.println(this.getClass() + " : " + parent);
      return parent;
    }
    public void debug() { }
  }
}
