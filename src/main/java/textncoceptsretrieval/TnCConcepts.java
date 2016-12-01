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
import java.io.BufferedReader;
import java.io.Reader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.FileUtils;
import org.jdom2.JDOMException;
import ixa.kaflib.KAFDocument;
import ixa.kaflib.Mark;
import ixa.kaflib.ExternalRef;
import org.springframework.web.multipart.MultipartFile;


public class TnCConcepts {
    

    public TnCConcepts(){
    }

    
    public final ResultConcepts getConcepts(MultipartFile doc, String lang, String markSource, String markUrl, String debug){

	long startTime = System.nanoTime();
	String warning = "";
	List<String> listConcepts = new ArrayList<String>();

	try{
	    File tmpFile = File.createTempFile("doc", ".tmp");
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

	    List<Mark> markables = naf.getMarks(markSource);
	    if(markables.size() == 0){
		warning += " No wikipedia concepts.";
	    }
	    for(Mark mark : markables){
		ExternalRef extRef1 = mark.getExternalRefs().get(0);
		if(lang.equals("en")){
		    listConcepts.add(extRef1.getReference().replace(markUrl,""));
		}
		else if(lang.equals("es") && extRef1.getExternalRefs().size() > 0){
		    listConcepts.add(extRef1.getExternalRefs().get(0).getReference().replace(markUrl,""));
		}
	    }
	    
	} catch (JDOMException e){
	    e.printStackTrace();
	} catch (IOException e){
	    e.printStackTrace();
	} catch (Exception e){
	    e.printStackTrace();
	}
	finally{
	    long difference = System.nanoTime() - startTime;
	    long hours = TimeUnit.NANOSECONDS.toHours(difference);
	    difference -= TimeUnit.HOURS.toNanos(hours);
	    long minutes = TimeUnit.NANOSECONDS.toMinutes(difference);
	    difference -= TimeUnit.MINUTES.toNanos(minutes);
	    long seconds = TimeUnit.NANOSECONDS.toSeconds(difference);
	    
	    String time = String.format("%d h, %d min, %d sec", hours, minutes, seconds);
	    ResultConcepts resultConcepts = new ResultConcepts(listConcepts, time, warning);
	    
	    return resultConcepts;
	}
    }

}
