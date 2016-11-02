/*
 * Copyright (C) 2016 IXA Taldea, University of the Basque Country UPV/EHU

   This file is part of TextnConceptsRetrieval.

   TextnConceptsRetrieval is free software: you can redistribute it
   and/or modify it under the terms of the GNU General Public License
   as published by the Free Software Foundation, either version 3 of
   the License, or (at your option) any later version.

   TextnConceptsRetrieval is distributed in the hope that it will be
   useful, but WITHOUT ANY WARRANTY; without even the implied warranty
   of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
   General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with TextnConceptsRetrieval.  If not, see
   <http://www.gnu.org/licenses/>.
*/


package textnconceptsretrieval;

import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.solr.client.solrj.*;
import org.apache.solr.common.*;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.itadaki.bzip2.BZip2InputStream;
import org.jdom2.JDOMException;
import ixa.kaflib.KAFDocument;
import ixa.kaflib.Mark;
import ixa.kaflib.WF;


public class TnCIndex {
    
    private SolrjClient solrjClient;


    public TnCIndex(String host, String port, String index){
	solrjClient = new SolrjClient(host, port, index);
    }


    public final void index(String docList, String type, String expdir, int nwords, int nexp, String shorter, String index, boolean commit){

	long startTime = System.nanoTime();

	try{
	    BufferedReader brDocs = new BufferedReader(new FileReader(docList));
	    String doc = null;
	    SolrInputDocument solrDoc = null;
	    int nDocs = 0;
	    while((doc = brDocs.readLine()) != null){
		System.err.println("Indexing document " + doc);
		solrDoc = new SolrInputDocument();
		String docId = FilenameUtils.getBaseName(doc);
		if(doc.contains("bz2")){
		    docId = FilenameUtils.getBaseName(docId);
		}
		solrDoc.addField("id", docId);
		String rawText = "";

		if(type.equals("text") && (nwords == -1)){
		    // Get and index raw text
		    File file = new File(doc);
		    rawText = FileUtils.readFileToString(file, "UTF-8");
		}
		else if((type.equals("concepts") || type.equals("expansion")) && (nwords == -1)){
		    // Get and index raw and wikification (from naf files)
		    String wikiText = "";
		    Reader reader;
		    if(doc.contains("bz2")){
			InputStream nafFileStream = new BufferedInputStream (new FileInputStream (doc));
			BZip2InputStream nafInputStream = new BZip2InputStream (nafFileStream, false);
			reader = new BufferedReader(new InputStreamReader(nafInputStream));
		    }
		    else{
			reader = new BufferedReader(new FileReader(doc));
		    }
			
		    KAFDocument naf = KAFDocument.createFromStream(reader);
		    int offset = 0;
		    List<WF> wfs = naf.getWFs();
		    for (int i = 0; i < wfs.size(); i++) {
			WF wf = wfs.get(i);
			if (offset != wf.getOffset()){
			    while(offset < wf.getOffset()) {
				rawText += " ";
				offset += 1;
			    }
			}
			rawText += wf.getForm();
			offset += wf.getLength();
		    }
		    List<Mark> markables = naf.getMarks("DBpediaaaaaa");
		    for(Mark mark : markables){
			String concept = mark.getExternalRefs().get(0).getReference().replace("http://dbpedia.org/resource/","");
			wikiText += " " + concept;
		    }
		    if(wikiText.equals("")){
			System.err.println("[WARNING] No wikipedia concepts for document " + docId);
		    }
		    if(type.equals("expansion")){
			String ppvfile = expdir + "/" + docId + ".ppv";
			if(! new File(ppvfile).exists()) {
			    System.err.println("[WARNING] " + ppvfile + " expansion file not found");
			}
			else{
			    BufferedReader expFile = new BufferedReader(new FileReader(ppvfile));
			    String exp = null;
			    int i = 0;
			    while((exp = expFile.readLine()) != null){
				if(exp.startsWith("!! -v")){
				    continue;
				}
				if(i==nexp)
				    break;
				wikiText += " " + exp.split("\t")[0];	
				i++;
			    }
			}
		    }
		    solrDoc.addField("concepts", wikiText);
		}
		else{ // Index only first N or last N tokens for each NAF document
		    Reader reader;
		    if(doc.contains("bz2")){
			InputStream nafFileStream = new BufferedInputStream (new FileInputStream (doc));
			BZip2InputStream nafInputStream = new BZip2InputStream (nafFileStream, false);
			reader = new BufferedReader(new InputStreamReader(nafInputStream));
		    }
		    else{
			reader = new BufferedReader(new FileReader(doc));
		    }
			
		    KAFDocument naf = KAFDocument.createFromStream(reader);
		    List<WF> wfs = naf.getWFs();
		    int ntokensDoc = wfs.size();
		    int begin = -1;
		    int end = -1;
		    if(shorter.equals("first")){
			begin = 0;
			if(nwords < ntokensDoc){
			    end = nwords - 1;
			}
			else{
			    end = ntokensDoc - 1;
			}
		    }
		    else{
			end = ntokensDoc - 1;
			if(nwords < ntokensDoc){
			    begin = ntokensDoc - nwords;
			}
			else{
			    begin = 0;
			}
		    }

		    int offset = wfs.get(begin).getOffset();
		    String firstWfId = "";
		    String lastWfId = "";
		    for (int i = begin; i < end; i++) {
			WF wf = wfs.get(i);
			if(i == begin){
			    firstWfId = wf.getId();
			}
			lastWfId = wf.getId();
			if (offset != wf.getOffset()){
			    while(offset < wf.getOffset()) {
				rawText += " ";
				offset += 1;
			    }
			}
			rawText += wf.getForm();
			offset += wf.getLength();
		    }
		    if(type.equals("concepts") || type.equals("expansion")){
			String wikiText = "";
			List<Mark> markables = naf.getMarks("DBpediaaaaaa");
			for(Mark mark : markables){
			    //int markWfId = Integer.parseInt(mark.getSpan().getFirstTarget().getId().replace("w",""));
			    //if(shorter.equals("first") && (Integer.parseInt(lastWfId.replace("w","")) < markWfId)){
			    //break;
			    //}
			    //if(shorter.equals("last") && (Integer.parseInt(firstWfId.replace("w","")) > markWfId)){
			    //continue;
			    //}
			    String concept = mark.getExternalRefs().get(0).getReference().replace("http://dbpedia.org/resource/","");
			    wikiText += " " + concept;
			}
			if(type.equals("expansion")){
			    String ppvfile = expdir + "/" + docId + ".ppv";
			    if(! new File(ppvfile).exists()) {
				System.err.println("[WARNING] " + ppvfile + " expansion file not found");
			    }
			    else{
				BufferedReader expFile = new BufferedReader(new FileReader(ppvfile));
				String exp = null;
				int i = 0;
				while((exp = expFile.readLine()) != null){
				    if(exp.startsWith("!! -v")){
					continue;
				    }
				    if(i==nexp)
					break;
				    wikiText += " " + exp.split("\t")[0];	
				    i++;
				}
			    }
			}

			if(wikiText.equals("")){
			    System.err.println("[WARNING] No wikipedia concepts for document " + docId);
			}
			solrDoc.addField("concepts", wikiText);
		    }

		}

		solrDoc.addField("text", rawText);
		solrjClient.index(solrDoc);
		nDocs++;
		if(nDocs%1000 == 0){
		    System.err.println("---" + nDocs + " docs indexed");
		}

		if(commit){
		    if(nDocs%5000 == 0){
			solrjClient.commit();
			System.err.println("+++ Committing Solr index " + index);
		    }
		}
		
	    }
	    System.err.println(nDocs + " documents indexed.");
	    if(commit){
		if(nDocs%5000 == 0){
		    solrjClient.commit();
		    System.err.println("+++ Committing Solr index " + index);
		}
	    }
 
	} catch(FileNotFoundException e) {
	    e.printStackTrace();
	} catch(IOException e){
	    e.printStackTrace();
	} catch(JDOMException e){
	    e.printStackTrace();
	}
	finally{
	    solrjClient.close();

	    long difference = System.nanoTime() - startTime;
	    long hours = TimeUnit.NANOSECONDS.toHours(difference);
	    difference -= TimeUnit.HOURS.toNanos(hours);
	    long minutes = TimeUnit.NANOSECONDS.toMinutes(difference);
	    difference -= TimeUnit.MINUTES.toNanos(minutes);
	    long seconds = TimeUnit.NANOSECONDS.toSeconds(difference);
	    System.err.println("Time spent: " + String.format("%d h, %d min, %d sec", hours, minutes, seconds));
	}
    }
  
}