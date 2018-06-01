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
 package com.ibm.ws.wlp.docgen.asciidoc;

import java.util.Map;

public class Attribute implements Comparable {

    public String name;
    public boolean optional;
    public String defaultValue;
    public String typeName;
    public Child type;
    public String desc;
    public Map<String, String> enumValues;
    public String ref;
    public String variable;
    public String min;
    public String max;
    public String breadcrum;
    public String group = "";

    public int compareTo(Object other) {
      Attribute o = (Attribute)other;

      int groupCompare = group.compareTo(o.group);
      if (groupCompare == 0) {
        return name.compareTo(o.name);
      } else {
        return groupCompare;
      }
    }
}
