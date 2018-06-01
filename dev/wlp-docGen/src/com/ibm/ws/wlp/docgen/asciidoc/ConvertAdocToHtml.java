/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2013, 2018
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.wlp.docgen.asciidoc;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import org.asciidoctor.Options;
import org.asciidoctor.Asciidoctor;
import org.asciidoctor.extension.JavaExtensionRegistry;

import com.ibm.ws.wlp.docgen.asciidoc.macros.ConfigFeatureLinkMacro;
import com.ibm.ws.wlp.docgen.asciidoc.macros.JavadocLinkMacro;

public class ConvertAdocToHtml extends Task {

  private File src;
  private File dest;
  private File javadocs;
  private Asciidoctor ad;
//  private String css;

  public void execute() {
    validate();

    try {
      List<File> adocFiles = new ArrayList<File>();
      collect(src, adocFiles);

      int count = 0;
      int allFiles = adocFiles.size();

      MemoryMXBean mem = ManagementFactory.getMemoryMXBean();
      long bytes = mem.getHeapMemoryUsage().getUsed();
      System.out.println("memory used " + (bytes / (1024*1024)) + "Mb");

      initAsciidoc();
      System.out.println("asciidoctorj initialized memory used " + (bytes / (1024*1024)) + "Mb");
      for (File srcFile : adocFiles) {
        convert(srcFile, getDestFile(srcFile));

        count++;

        if (count % 100 == 0) {
          bytes = mem.getHeapMemoryUsage().getUsed();
          System.out.println("converted " + count + " of " + allFiles + " memory used " + (bytes / (1024*1024)) + "Mb");
        }
      }

      ad.shutdown();
    } catch (IOException ioe) {
      throw new BuildException(ioe.getMessage(), ioe);
    }
  }

  private void initAsciidoc() {
    ad = Asciidoctor.Factory.create();
    JavaExtensionRegistry registry = ad.javaExtensionRegistry();
    registry.inlineMacro(new ConfigFeatureLinkMacro("config"));
    registry.inlineMacro(new ConfigFeatureLinkMacro("feature"));
    registry.inlineMacro(new JavadocLinkMacro("javadoc", javadocs));

  }

  private File getDestFile(File srcFile) throws IOException {
    Path filePath = srcFile.toPath();
    Path srcPath = src.toPath();
    Path relative = srcPath.relativize(filePath);
    return dest.toPath().resolve(relative).toFile().getParentFile();
  }

  private void collect(File src, List<File> adocFiles) {
    if (src.isDirectory()) {
      File[] files = src.listFiles();
      if (files != null) {
        for (File f : files) {
          collect(f, adocFiles);
        }
      }
    } else if (src.isFile() && src.getName().endsWith(".adoc")) {
      adocFiles.add(src);
    }
  }

  private void process(File src2, File dest2) throws IOException {
    if (src2.isDirectory()) {
      File newDest = new File(dest2, src2.getName());
      newDest.mkdirs();
      File[] files = src2.listFiles();
      if (files != null) {
        for (File f : files) {
          process(f, newDest);
        }
      }
    } else if (src2.isFile() && src2.getName().endsWith(".adoc")) {
      convert(src2, dest2);
    }
  }

  private void convert(File src2, File dest2) throws IOException {
    dest2.mkdirs();
    Options options = new Options();
    options.setOption("to_dir", dest2.getAbsolutePath());

    ad.convertFile(src2, options);
  }

  private String getString(StringBuilder builder, int index2, String tag) {
    int startIndex = builder.indexOf('<' + tag + '>', index2);
    int endIndex = builder.indexOf("</" + tag + '>', startIndex);
    return builder.substring(startIndex + tag.length() + 2, endIndex);
  }

  private void validate() {
    if (src == null) {
      throw new BuildException("src dir must be specified");
    }

    if (dest == null) {
      throw new BuildException("dest dir must be specified");
    }

    if (!!!src.exists()) {
      throw new BuildException("src dir " + src + " does not exist");
    }

    if (javadocs != null && !!!javadocs.exists()) {
      throw new BuildException("javadoc dir " + javadocs + " does not exist");
    }

    if (!!!dest.exists() && !!!dest.mkdirs()) {
      throw new BuildException("dest dir " + dest + " does not exist");
    }

    if (!!!src.isDirectory()) {
      throw new BuildException("src dir " + src + " is not a directory");
    }

    if (javadocs != null && !!!javadocs.isDirectory()) {
      throw new BuildException("javadoc dir " + javadocs + " is not a directory");
    }

    if (!!!dest.isDirectory()) {
      throw new BuildException("dest dir " + dest + " is not a directory");
    }

  }

  public void setSrc(File src) {
    this.src = src;
  }
  public void setDest(File dest) {
    this.dest = dest;
  }

  public void setJavadocDir(File dir) {
    javadocs = dir;
  }
}
