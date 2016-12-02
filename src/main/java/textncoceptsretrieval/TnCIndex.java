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
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.solr.common.SolrInputDocument;
import org.jdom2.JDOMException;
import ixa.kaflib.KAFDocument;
import ixa.kaflib.Mark;
import ixa.kaflib.WF;
import ixa.kaflib.ExternalRef;
import org.springframework.web.multipart.MultipartFile;


public class TnCIndex {
    
    private SolrjClient solrjClient;
    

    public TnCIndex(String host, String port, String index){
	solrjClient = new SolrjClient(host, port, index);
    }

    
    public final ResultIndex index(MultipartFile doc, String id, String lang, String markSource, String markUrl, int nwords, String shorter, String index, String debug){

	long startTime = System.nanoTime();
	String warning = "";
	int numIndexed = 0;

	try{
	    SolrInputDocument solrDoc = new SolrInputDocument();
	    solrDoc.addField("id", id);
	    String rawText = "";
	    String wikiText = "";

	    File tmpFile = File.createTempFile("query", ".tmp");
	    tmpFile.deleteOnExit();
	    String tmpFileName = tmpFile.getAbsolutePath();
	    FileUtils.writeStringToFile(tmpFile, IOUtils.toString(new ByteArrayInputStream(doc.getBytes()), "UTF-8"));
	    
	    // run ixa-pipes
	    String ixaPipes = "cat " + tmpFileName + " | ixa-pipe-tok-" + lang + ".sh | ixa-pipe-pos-" + lang + ".sh | ixa-pipe-wikify-" + lang + ".sh";
	    String[] cmdPipes = {
		"/bin/sh",
		"-c",
		ixaPipes
	    };
	    Process pPipes = Runtime.getRuntime().exec(cmdPipes);
		    
	    String outputPipes = "";
	    String outputLinePipes = "";
	    Reader reader = new BufferedReader(new InputStreamReader(pPipes.getInputStream(), "UTF-8"));
	    KAFDocument naf = KAFDocument.createFromStream(reader);
	    
	    if(debug.equals("true")){
		String errorPipes = "";
		BufferedReader errorPipesStream = new BufferedReader(new InputStreamReader(pPipes.getErrorStream()));
		while((errorPipes = errorPipesStream.readLine()) != null){
		    warning += "[IXA-PIPES] " + errorPipes;
		}
		errorPipesStream.close();
	    }

	    pPipes.waitFor();

	    List<WF> wfs = naf.getWFs();
	    if(nwords == -1){
		int offset = 0;
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
	    }
	    else{ // Index only first N or last N tokens for each NAF document
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
	    }
	    solrDoc.addField("text_" + lang, rawText);

	    List<Mark> markables = naf.getMarks(markSource);
	    for(Mark mark : markables){
		ExternalRef extRef1 = mark.getExternalRefs().get(0);
		if(lang.equals("en")){
		    wikiText += extRef1.getReference().replace(markUrl,"") + " ";
		}
		else if(lang.equals("es") && extRef1.getExternalRefs().size() > 0){
		    wikiText += extRef1.getExternalRefs().get(0).getReference().replace(markUrl,"") + " ";
		}
	    }
	    if(wikiText.equals("")){
		warning += " No wikipedia concepts for this document.";
	    }
	    solrDoc.addField("concepts", wikiText);
	    solrjClient.index(solrDoc);

	    numIndexed = 1;
		    
	} catch(IOException e){
	    e.printStackTrace();
	} catch(JDOMException e){
	    e.printStackTrace();
	} catch(Exception e){
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

	    String time = String.format("%d h, %d min, %d sec", hours, minutes, seconds);
	    ResultIndex resultIndex = new ResultIndex(numIndexed, time, warning);

	    return resultIndex;
	}
    }


    public final ResultIndex indexdir(String docDir, String lang, String markSource, String markUrl, int nwords, String shorter, String index, String debug){

	long startTime = System.nanoTime();
	String warning = "";
	int numIndexed = 0;

	try{
	    String doc = null;
	    SolrInputDocument solrDoc = null;

	    File directory = new File(docDir);
	    if(!directory.isDirectory()){
		warning += docDir + " directory not found. ";
		throw new FileNotFoundException();
	    }
	    File[] fList = directory.listFiles();
	    for (File file : fList){
		if (file.isFile()){
		    System.err.println("Indexing document " + file.getName());

		    solrDoc = new SolrInputDocument();
		    String docId = FilenameUtils.getBaseName(file.getName());
		    solrDoc.addField("id", docId);
		    String rawText = "";
		    String wikiText = "";

		    // run ixa-pipes
		    String ixaPipes = "cat " + docDir + "/" + file.getName() + " | ixa-pipe-tok-" + lang + ".sh | ixa-pipe-pos-" + lang + ".sh | ixa-pipe-wikify-" + lang + ".sh";
		    String[] cmdPipes = {
			"/bin/sh",
			"-c",
			ixaPipes
		    };
		    Process pPipes = Runtime.getRuntime().exec(cmdPipes);
		    
		    String outputPipes = "";
		    String outputLinePipes = "";
		    Reader reader = new BufferedReader(new InputStreamReader(pPipes.getInputStream(), "UTF-8"));
		    KAFDocument naf = KAFDocument.createFromStream(reader);

		    if(debug.equals("true")){
			String errorPipes = "";
			BufferedReader errorPipesStream = new BufferedReader(new InputStreamReader(pPipes.getErrorStream()));
			while((errorPipes = errorPipesStream.readLine()) != null){
			    warning += "[IXA-PIPES] " + errorPipes;
			}
			errorPipesStream.close();
		    }

		    pPipes.waitFor();

		    List<WF> wfs = naf.getWFs();
		    if(nwords == -1){
			int offset = 0;
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
		    }
		    else{ // Index only first N or last N tokens for each NAF document
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
		    }
		    solrDoc.addField("text_" + lang, rawText);

		    List<Mark> markables = naf.getMarks(markSource);
		    for(Mark mark : markables){
			ExternalRef extRef1 = mark.getExternalRefs().get(0);
			if(lang.equals("en")){
			    wikiText += extRef1.getReference().replace(markUrl,"") + " ";
			}
			else if(lang.equals("es") && extRef1.getExternalRefs().size() > 0){
			    wikiText += extRef1.getExternalRefs().get(0).getReference().replace(markUrl,"") + " ";
			}
		    }
		    if(wikiText.equals("")){
			warning += " No wikipedia concepts for document " + docId + ".";
		    }
		    solrDoc.addField("concepts", wikiText);

		    solrjClient.index(solrDoc);
		    numIndexed++;
		    if(numIndexed%1000 == 0){
			System.err.println("---" + numIndexed + " docs indexed");
		    }
		}
	    }
	    
	} catch(FileNotFoundException e) {
	    e.printStackTrace();
	} catch(IOException e){
	    e.printStackTrace();
	} catch(JDOMException e){
	    e.printStackTrace();
	} catch(Exception e){
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

	    String time = String.format("%d h, %d min, %d sec", hours, minutes, seconds);
	    ResultIndex resultIndex = new ResultIndex(numIndexed, time, warning);

	    return resultIndex;
	}
    }



















   
}
