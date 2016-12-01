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

import java.io.FileReader;
import java.io.File;
import java.io.BufferedReader;
import java.io.Reader;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.solr.client.solrj.*;
import org.apache.solr.common.*;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.jdom2.JDOMException;
import ixa.kaflib.KAFDocument;
import ixa.kaflib.Mark;
import ixa.kaflib.WF;
import ixa.kaflib.ExternalRef;
import org.springframework.web.multipart.MultipartFile;


public class TnCQuery {
    
    private SolrjClient solrjClient;

    public TnCQuery(String host, String port, String index){
	solrjClient = new SolrjClient(host, port, index);
    }

    
    public final ResultQuery runQuery(MultipartFile qFile, String lang, String index, String type, String markSource, String markUrl, int nwords, String shorter, int ndocs, String debug){

	long startTime = System.nanoTime();
	String warning = "";
	int numFound = 0;
	List<ResultDoc> listResultDocs = new ArrayList<ResultDoc>();

	try{
	    String qfield = "";
	    if(type.equals("mono")){
		if(lang.equals("en")){
		    qfield = "text_en";
		}
		else if(lang.equals("es")){
		    qfield = "text_es";
		}
	    }
	    else if(type.equals("cross")){
		if(lang.equals("en")){
		    qfield = "text_en^1.0 concepts^1.0";
		}
		else if(lang.equals("es")){
		    qfield = "text_es^1.0 concepts^1.0";
		}
	    }


	    String docname = FilenameUtils.getBaseName(qFile.getOriginalFilename());

	    String query = "";
	    if(type.equals("mono") && (nwords == -1)){
		query = IOUtils.toString(new ByteArrayInputStream(qFile.getBytes()), "UTF-8");
	    }
	    else{ // (type = 'cross') OR (type = 'mono' AND nwords != -1). So, extract query from a NAF document
		File tmpFile = File.createTempFile("query", ".tmp");
		tmpFile.deleteOnExit();
		String tmpFileName = tmpFile.getAbsolutePath();
		FileUtils.writeStringToFile(tmpFile, IOUtils.toString(new ByteArrayInputStream(qFile.getBytes()), "UTF-8"));

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
		int ntokensDoc = wfs.size();
		int begin = -1;
		int end = -1;
		if(nwords == -1){
		    begin = 0;
		    end = ntokensDoc;
		}
		else if(shorter.equals("first")){
		    begin = 0;
		    if(nwords < ntokensDoc){
			end = nwords;
		    }
		    else{
			end = ntokensDoc;
		    }
		}
		else{
		    end = ntokensDoc;
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
			    query += " ";
			    offset += 1;
			}
		    }
		    query += wf.getForm();
		    offset += wf.getLength();
		}

		if(type.equals("cross")){
		    List<Mark> markables = naf.getMarks(markSource);
		    if(markables.size() == 0){
			warning += " No wikipedia concepts.";
		    }
		    for(Mark mark : markables){
			ExternalRef extRef1 = mark.getExternalRefs().get(0);
			if(lang.equals("en")){
			    query += " " + extRef1.getReference().replace(markUrl,"");
			}
			else if(lang.equals("es") && extRef1.getExternalRefs().size() > 0){
			    query += " " + extRef1.getExternalRefs().get(0).getReference().replace(markUrl,"");
			}
		    }
		}	
	    }

	    SolrQuery solrQuery = new SolrQuery();
	    // Escape Solr special characters
	    // + - && || ! ( ) { } [ ] ^ " ~ * ? : \ /
	    // ClientUtils.escapeQueryChars(S) not working properly
	    query = query.replace("\\","\\\\");  
	    query = query.replace("+","\\+");
	    query = query.replace("-","\\-");
	    query = query.replace("&&","\\&&");
	    query = query.replace("||","\\||");
	    query = query.replace("!","\\!");
	    query = query.replace("(","\\(");
	    query = query.replace(")","\\)");
	    query = query.replace("{","\\{");
	    query = query.replace("}","\\}");
	    query = query.replace("[","\\[");
	    query = query.replace("]","\\]");
	    query = query.replace("^","\\^");
	    query = query.replace("\"","\\\"");
	    query = query.replace("~","\\~");
	    query = query.replace("*","\\*");
	    query = query.replace("?","\\?");
	    query = query.replace(":","\\:");
	    query = query.replace("/","\\/");
	    
	    solrQuery.set("q",query);
	    solrQuery.set("qf",qfield);
	    solrQuery.set("defType","dismax");
	    solrQuery.set("fl","id,score");
	    solrQuery.setIncludeScore(true);
	    solrQuery.setRows(ndocs + 1);	
	    
	    QueryResponse response = solrjClient.query(solrQuery);  

	    if(debug.equals("true")){
		warning += "[SOLR query parameters] " + (String)response.getHeader().toString();
	    }

	    SolrDocumentList list = response.getResults();

	    if(list.getNumFound() == 0){
		warning += " No docs found for the query.";
		numFound = 0;
	    }
	    
	    int rank = 1;
	    for (SolrDocument solrDoc: list){
		if(rank > ndocs)
		    break;
		String docId = (String)solrDoc.getFieldValue("id");
		if(docId.equals(docname)){
		    if(list.getNumFound() == 1){
			warning += "Only 1 doc (itself) found for the query.";
			numFound = 0;
		    }
		    continue;
		}
		
		ResultDoc resultDoc = new ResultDoc((Float)solrDoc.getFieldValue("score"),docId);
		listResultDocs.add(resultDoc);
		
		rank++;
	    }
	    numFound = rank -1;
	    
	} catch (JDOMException e){
	    e.printStackTrace();
	} catch (IOException e){
	    e.printStackTrace();
	} catch (Exception e){
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
	    ResultQuery resultQuery = new ResultQuery(listResultDocs, numFound, time, warning);
	    
	    return resultQuery;
	}
    }

}
