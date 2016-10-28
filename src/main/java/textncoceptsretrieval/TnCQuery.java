/*
 * Copyright (C) 2015 IXA Taldea, University of the Basque Country UPV/EHU

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
import org.itadaki.bzip2.BZip2InputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.FilenameUtils;
import org.apache.solr.client.solrj.*;
import org.apache.solr.common.*;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.jdom2.JDOMException;
import ixa.kaflib.KAFDocument;
import ixa.kaflib.Mark;
import ixa.kaflib.WF;


public class TnCQuery {
    
    private SolrjClient solrjClient;

    public TnCQuery(String host, String port, String index){
	solrjClient = new SolrjClient(host, port, index);
    }


    public final void runQueries(String docs4queryingList, String index, String type, String markSource, String markUrl, String expdir, int nwords, int nexp, String shorter, boolean debug){

	long startTime = System.nanoTime();

	try{
	    String qfield = type;
	    if(type.equals("all")){
		qfield = "text^1.0 concepts^1.0";
	    }
	    if(type.equals("expansion")){
		qfield = "concepts";
	    }

	    BufferedReader brDocs = new BufferedReader(new FileReader(docs4queryingList));
	    String doc = null;
	    String docname = "";
	    while((doc = brDocs.readLine()) != null){
		if(!new File(doc).exists()) { 
		    System.err.println("[WARNING] Document not found " + docname);
		    System.err.println("[WARNING] 0 docs found for query " + docname);
		    System.out.println(docname + " 0 " + "XXXXXXXX 1 0.0 " + index + "_f-" + qfield);
		    continue;
		}

		docname = FilenameUtils.getBaseName(doc);
		if(doc.contains("bz2")){
		    docname = FilenameUtils.getBaseName(docname);
		}

		String query = "";
		if(type.equals("text") && (nwords == -1)){
		    BufferedReader br = new BufferedReader(new FileReader(doc));
		    StringBuffer text = new StringBuffer();
		    String line = null;
		    while((line = br.readLine()) != null ){
			text.append( line );
		    }
		    query = text.toString();
		}
		else{ // (type = 'concepts', 'expansion' or 'all') OR (type = 'text' AND nwords != -1). So, extract query from NAF document
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
				if((type.equals("all")) || (type.equals("text"))){
				    query += " ";
				}
				offset += 1;
			    }
			}
			if((type.equals("all")) || (type.equals("text"))){
			    query += wf.getForm();
			}
			offset += wf.getLength();
		    }

		    if((type.equals("all")) || (type.equals("concepts")) || (type.equals("expansion"))){
			List<Mark> markables = naf.getMarks(markSource);
			if(markables.size() == 0){
			    System.err.println("[WARNING] No wikipedia concepts for query " + docname);
			}
			for(Mark mark : markables){
			    //int markWfId = Integer.parseInt(mark.getSpan().getFirstTarget().getId().replace("w",""));
			    //if(shorter.equals("first") && (Integer.parseInt(lastWfId.replace("w","")) < markWfId)){
			    //	break;
			    //}
			    //if(shorter.equals("last") && (Integer.parseInt(firstWfId.replace("w","")) > markWfId)){
			    //continue;
			    //}
			    String concept = mark.getExternalRefs().get(0).getReference().replace(markUrl,"");
			    query += " " + concept;
			}
			if(type.equals("expansion") || type.equals("all")){
			    String ppvfile = expdir + "/" + docname + ".ppv";
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
				    query += " " + exp.split("\t")[0];	
				    i++;
				}
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
		solrQuery.setRows(1001);
		
		QueryResponse response = solrjClient.query(solrQuery);  
		if(debug){
		    System.err.println("Query " + docname + " parameters: " + (String)response.getHeader().toString());
		}
		SolrDocumentList list = response.getResults();

		if(list.getNumFound() == 0){
		    System.err.println("[WARNING] 0 docs found for query " + docname);
		    System.out.println(docname + " 0 " + "XXXXXXXX 1 0.0 " + index + "_f-" + qfield);
		    continue;
		}

		int rank = 1;
		for (SolrDocument solrDoc: list){
		    if(rank>1000)
			break;
		    String docId = (String)solrDoc.getFieldValue("id");
		    if(docId.equals(docname)){
			if(list.getNumFound() == 1){
			    System.err.println("[WARNING] Only 1 doc (itself) found for query " + docname);
			    System.out.println(docname + " 0 " + "XXXXXXXX 1 0.0 " + index + "_f-" + qfield);
			}
			continue;
		    }

		    System.out.println(docname + " 0 " + docId + " " + rank + " " + solrDoc.getFieldValue("score") + " " + index + "_f-" + qfield);
		    rank++;
		}
	    }
	   
	} catch (JDOMException e){
	    e.printStackTrace();
	} catch (IOException e){
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