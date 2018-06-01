package com.ibm.ws.wlp.docgen.messages;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.tools.ant.Task;

public class MergeMessages extends Task {
  
  private static final Pattern MESSAGE_ID_PATTERN = Pattern.compile("([a-zA-Z]{4,5}).*");

  private File messagesDir;
  
  public void execute() {
    File[] files = messagesDir.listFiles(new FileFilter() {
      
      @Override
      public boolean accept(File pathname) {
        return pathname.isFile() && pathname.getName().endsWith(".xml");
      }
    });
    
    Map<String, List<File>> filesByPrefix = new HashMap<String, List<File>>();
    
    XMLInputFactory inputFactory = XMLInputFactory.newInstance();
    XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
    
    if (files != null) {
      nextFile: for (File f : files) {
        try {
          XMLStreamReader reader = inputFactory.createXMLStreamReader(new StreamSource(f));
          while (reader.hasNext()) {
            if (reader.nextTag() == XMLStreamReader.START_ELEMENT &&
                "Message".equals(reader.getLocalName())) {
              int attributeCount = reader.getAttributeCount();
              for (int i = 0; i < attributeCount; i++) {
                if ("ID".equals(reader.getAttributeLocalName(i))) {
                  String messageId = reader.getAttributeValue(i);
                  Matcher m = MESSAGE_ID_PATTERN.matcher(messageId);
                  if (m.matches()) {
                    String idPrefix = m.group(1);
                    List<File> matchingFiles = filesByPrefix.get(idPrefix);
                    if (matchingFiles == null) {
                      matchingFiles = new ArrayList<File>();
                      filesByPrefix.put(idPrefix, matchingFiles);
                    }
                    matchingFiles.add(f);
                    reader.close();
                    continue nextFile;
                  } else {
                    System.err.println("Non matching message id " + messageId + " from " + f);
                    // TODO work out if I should ignore or blow the build up
                  }
                }
              }
            }
          }
        } catch (XMLStreamException e) {
          // TODO work out if we should ignore, or blow the build up.
          e.printStackTrace();
        }
      }
    
      for (Map.Entry<String, List<File>> entry : filesByPrefix.entrySet()) {
        List<File> filesToMerge = entry.getValue();
        // if there are zero or one we don't have to merge.
        if (filesToMerge.size() > 1) {
          // need to merge
          File output = new File(messagesDir, entry.getKey() + ".xml");
          try {
            output.createNewFile();
            XMLEventWriter writer = outputFactory.createXMLEventWriter(new StreamResult(new FileOutputStream(output)));
            boolean capture = false;
            boolean first = true;
            List<XMLEvent> endEvents = new ArrayList<XMLEvent>();
            for (File f : filesToMerge) {
              XMLEventReader reader = inputFactory.createXMLEventReader(new StreamSource(f));
              boolean copyEvents = false;
              while (reader.hasNext()) {
                XMLEvent event = reader.nextEvent();
                
                boolean copyThisEvent = copyEvents;
                
                if (!!!first) {
                  // work out if we should copy yet.
                  if (event.isStartElement() && "Message".equals(event.asStartElement().getName().getLocalPart())) {
                    copyThisEvent = copyEvents = true;
                  } else if (event.isEndElement() && "Message".equals(event.asEndElement().getName().getLocalPart())) {
                    copyEvents = false;
                  }
                } else if (capture){
                  // Look for the end events and capture on first run through
                  endEvents.add(event);
                } else if (first && event.isEndElement() && "TMSSource".equals(event.asEndElement().getName().getLocalPart())) {
                  capture = true;
                  endEvents.add(event);
                } else if (first) {
                  copyThisEvent = true;
                }
                if (copyThisEvent) {
                  writer.add(event);
                }
              }
              capture = false;
              first = false;
              reader.close();
              f.delete();
            }
            for (XMLEvent event : endEvents) {
              writer.add(event);
            }
            
            writer.close();
            
          } catch (XMLStreamException e) {
            // TODO work out if I should ignore or blow up the build
            e.printStackTrace();
          } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }
        }
      }
    }
  }
  
  public void setDir(File dir) {
    messagesDir = dir;
  }
}
