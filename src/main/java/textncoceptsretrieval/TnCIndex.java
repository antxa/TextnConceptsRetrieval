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
import org.jdom2.JDOMException;
import ixa.kaflib.KAFDocument;
import ixa.kaflib.Mark;
import ixa.kaflib.WF;


public class TnCIndex {
    
    private SolrjClient solrjClient;
    

    public TnCIndex(String host, String port, String index){
	solrjClient = new SolrjClient(host, port, index);
    }

    
    public final ResultIndex index(String docDir, String lang, String type, String markSource, String markUrl, int nwords, String shorter, String index, String debug){

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

		    if(type.equals("text") && (nwords == -1)){
			// Get and index raw text
			rawText = FileUtils.readFileToString(file, "UTF-8");
		    }
		    else if((type.equals("concepts") || type.equals("concepts-naf")) && (nwords == -1)){
			// Get and index raw text and wikification (from NAF files)
			// concepts --> Get NAF document running ixa-pipes
			// concepts-naf --> The input document is already a NAF document
			String wikiText = "";
			Reader reader;
			if(type.equals("concepts-naf")){
			    reader = new BufferedReader(new FileReader(file.getName()));
			}
			else{ // type=concepts
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
			    reader = new BufferedReader(new InputStreamReader(pPipes.getInputStream(), "UTF-8"));

			    if(debug.equals("true")){
				String errorPipes = "";
				BufferedReader errorPipesStream = new BufferedReader(new InputStreamReader(pPipes.getErrorStream()));
				while((errorPipes = errorPipesStream.readLine()) != null){
				    warning += "[IXA-PIPES] " + errorPipes;
				}
				errorPipesStream.close();
			    }

			    pPipes.waitFor();

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
			List<Mark> markables = naf.getMarks(markSource);
			for(Mark mark : markables){
			    String concept = mark.getExternalRefs().get(0).getReference().replace(markUrl,"");
			    wikiText += " " + concept;
			}
			if(wikiText.equals("")){
			    warning += " No wikipedia concepts for document " + docId + ".";
			}
	   
			solrDoc.addField("concepts", wikiText);
		    }
		    else{ // Index only first N or last N tokens for each NAF document
			Reader reader;
			if(type.equals("concepts-naf")){ //The input document is already a NAF document
			    reader = new BufferedReader(new FileReader(file.getName()));
			}
			else{
			    // run ixa-pipes
			    String ixaPipes = "cat " + docDir + "/" + file.getName() + " | sh ixa-pipe-tok-" + lang + ".sh | sh ixa-pipe-pos-" + lang + ".sh |sh ixa-pipe-wikify-" + lang + ".sh";
			    String[] cmdPipes = {
				"/bin/sh",
				"-c",
				ixaPipes
			    };

			    Process pPipes = Runtime.getRuntime().exec(cmdPipes);

			    String outputPipes = "";
			    String outputLinePipes = "";
			    reader = new BufferedReader(new InputStreamReader(pPipes.getInputStream(), "UTF-8"));

			    if(debug.equals("true")){
				String errorPipes = "";
				BufferedReader errorPipesStream = new BufferedReader(new InputStreamReader(pPipes.getErrorStream()));
				while((errorPipes = errorPipesStream.readLine()) != null){
				    warning += "[IXA-PIPES] " + errorPipes;
				}
				errorPipesStream.close();
			    }

			    pPipes.waitFor();

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
			if(type.equals("concepts") || type.equals("concepts-naf")){
			    String wikiText = "";
			    List<Mark> markables = naf.getMarks(markSource);
			    for(Mark mark : markables){
				String concept = mark.getExternalRefs().get(0).getReference().replace(markUrl,"");
				wikiText += " " + concept;
			    }
			   
			    if(wikiText.equals("")){
				warning += " No wikipedia concepts for document " + docId + ".";
			    }
			    solrDoc.addField("concepts", wikiText);
			}

		    }
		    
		    solrDoc.addField("text", rawText);
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
