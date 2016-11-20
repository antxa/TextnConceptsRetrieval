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

import org.springframework.boot.SpringApplication;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.UnsatisfiedServletRequestParameterException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;


@SpringBootApplication
@RestController
public class Application {

    
    /**
     * POST HTTP request method that retrieves a list of documents in
     * JSON format given a query.
     * 
     * @param qfile  [Required] Text file sent as body parameter. The content of
     * the file (text) will be used as a query.
     * @param lang  [Optional] Language of the text in qfile. Choices:
     * 'en', 'es'. Default: 'en'.
     * @param type [Optional] Type of the query: 'text' (only raw
     * text), 'concepts' (only concepts), 'all' (text +
     * concepts). Choices: 'text', 'concepts', 'all'. Default: 'text'.
     * @param mark [Optional] Value of the 'source' attribute of
     * '<mark>' elements in NAF documents to be used as
     * concepts. Default: 'DBpedia_spotlight'. Required if 'type'
     * specified as 'concepts' or 'all'.
     * @param url  [Optional] Base of the URL in 'reference' attribute
     * of the '<externalRef>' element in NAF documents. Default:
     * 'http://dbpedia.org/resource/' for lang=en and
     * 'http://es.dbpedia.org/resource/' for lang=es. Required if
     * 'type' specified as 'concepts' or 'all'.
     * @param nwords  [Optional] Number of words to use for
     * querying. Default: -1 (all words).
     * @param shorter  [Optional] Makes the query shorter. FIRST or
     * LAST nwords of each query will be used for querying. Choices:
     * 'first', 'last'. Default: 'first'.
     * @param ndocs  [Optional] Number of documents to
     * retrieve. Default: 10.
     * @param host  [Optional] Hostname where Solr server is
     * running. Default: 'localhost'.
     * @param port  [Optional] Port number where Solr server is
     * running. Default: 8983.
     * @param index  [Optional] Solr index name. Default:
     * 'textnconcepts'.
     * @param debug [Optional] Choices: 'true', 'false'. Default:
     * 'false'.
     * @return ResultQuery object, including a list of document
     * id/score pair.
     */
    @RequestMapping(value = "/query", method = RequestMethod.POST)
    public ResultQuery query(@RequestParam(value="qfile") MultipartFile qfile,
			     @RequestParam(value="lang", required=false, defaultValue="en") String lang,
			     @RequestParam(value="type", required=false, defaultValue="text") String type,
			     @RequestParam(value="mark", required=false, defaultValue="DBpedia_spotlight") String markSource,
			     @RequestParam(value="url", required=false, defaultValue="") String markUrl,
			     @RequestParam(value="nwords", required=false, defaultValue="-1") String nwords,
			     @RequestParam(value="shorter", required=false, defaultValue="first") String shorter,
			     @RequestParam(value="ndocs", required=false, defaultValue="10") String ndocs,
			     @RequestParam(value="host", required=false, defaultValue="localhost") String host,
			     @RequestParam(value="port", required=false, defaultValue="8983") String port,
			     @RequestParam(value="index", required=false, defaultValue="textnconcepts") String index,
			     @RequestParam(value="debug", required=false, defaultValue="false") String debug)
	throws UnsatisfiedServletRequestParameterException {

	if(!lang.equals("en") && !lang.equals("es")){
	    String[] paramConditions = {"en","es"};
	    String[] actual = {lang};
	    Map<String,String[]> actualParams = new HashMap<String, String[]>();
	    actualParams.put("lang", actual);
	    throw new UnsatisfiedServletRequestParameterException(paramConditions,actualParams);
	}
	if(!type.equals("text") && !type.equals("concepts") && !type.equals("all")){
	    String[] paramConditions = {"text","concepts","all"};
	    String[] actual = {type};
	    Map<String,String[]> actualParams = new HashMap<String, String[]>();
	    actualParams.put("type", actual);
	    throw new UnsatisfiedServletRequestParameterException(paramConditions,actualParams);
	}
	if(!shorter.equals("first") && !shorter.equals("last")){
	    String[] paramConditions = {"first","last"};
	    String[] actual = {shorter};
	    Map<String,String[]> actualParams = new HashMap<String, String[]>();
	    actualParams.put("shorter", actual);
	    throw new UnsatisfiedServletRequestParameterException(paramConditions,actualParams);
	}
	if(!debug.equals("true") && !debug.equals("false")){
	    String[] paramConditions = {"true","false"};
	    String[] actual = {debug};
	    Map<String,String[]> actualParams = new HashMap<String, String[]>();
	    actualParams.put("debug", actual);
	    throw new UnsatisfiedServletRequestParameterException(paramConditions,actualParams);
	}

	if(markUrl.equals("")){
	    if(lang.equals("en")){
		markUrl = "http://dbpedia.org/resource/";
	    }
	    else if(lang.equals("es")){
		markUrl = "http://es.dbpedia.org/resource/";
	    }
	}


	TnCQuery tncQuery = new TnCQuery(host, port, index);
	ResultQuery result = tncQuery.runQuery(qfile, lang, index, type, markSource, markUrl, Integer.parseInt(nwords), shorter, Integer.parseInt(ndocs), debug);
	
	return result;
    }


    /**
     * GET or POST HTTP request method that indexes documents.
     * 
     * @param docsDir  [Required] Path to a directory containing the
     * documents to be indexed. It should be 'docs4indexing' or a
     * subdirectory inside this directory.
     * @param lang  [Optional] Language of the documents. Choices:
     * 'en', 'es'. Default: 'en'.
     * @param type  [Optional] Type of the indexation: 'text' (only raw
     * text) or 'concepts' (text + concepts). Choices: 'text',
     * 'concepts'. Default: 'text'.
     * @param mark  [Optional] Value of the 'source' attribute of
     * '<mark>' elements in NAF documents to be used as
     * concepts. Default: 'DBpedia_spotlight'. Required if 'type'
     * specified as 'concepts'.
     * @param url  [Optional] Base of the URL in 'reference' attribute
     * of the '<externalRef>' element in NAF documents. Default:
     * 'http://dbpedia.org/resource/' for lang=en and
     * 'http://es.dbpedia.org/resource/' for lang=es. Required if
     * 'type' specified as 'concepts'.
     * @param nwords [Optional] Number of words to index. Default: -1
     * (all words).
     * @param shorter [Optional] Makes the document to be indexed
     * shorter. FIRST or LAST nwords of each document will be
     * indexed. Choices: 'first', 'last'. Default: 'first'.
     * @param host  [Optional] Hostname where Solr server is
     * running. Default: 'localhost'.
     * @param port  [Optional] Port number where Solr server is
     * running. Default: 8983.
     * @param index  [Optional] Solr index name. Default:
     * 'textnconcepts'.
     * @param debug [Optional] Choices: 'true', 'false'. Default:
     * 'false'.
     * @return  ResultIndex object.
     */
    @RequestMapping(value = "/index")
    public ResultIndex index(@RequestParam(value="docsDir") String docsDir,
			     @RequestParam(value="lang", required=false, defaultValue="en") String lang,
			     @RequestParam(value="type", required=false, defaultValue="text") String type,
			     @RequestParam(value="mark", required=false, defaultValue="DBpedia_spotlight") String markSource,
			     @RequestParam(value="url", required=false, defaultValue="") String markUrl,
			     @RequestParam(value="nwords", required=false, defaultValue="-1") String nwords,
			     @RequestParam(value="shorter", required=false, defaultValue="first") String shorter,
			     @RequestParam(value="host", required=false, defaultValue="localhost") String host,
			     @RequestParam(value="port", required=false, defaultValue="8983" ) String port,
			     @RequestParam(value="index", required=false, defaultValue="textnconcepts") String index,
			     @RequestParam(value="debug", required=false, defaultValue="false") String debug)
	throws UnsatisfiedServletRequestParameterException {

	if(!lang.equals("en") && !lang.equals("es")){
	    String[] paramConditions = {"en","es"};
	    String[] actual = {lang};
	    Map<String,String[]> actualParams = new HashMap<String, String[]>();
	    actualParams.put("lang", actual);
	    throw new UnsatisfiedServletRequestParameterException(paramConditions,actualParams);
	}
	if(!type.equals("text") && !type.equals("concepts")){
	    String[] paramConditions = {"text","concepts"};
	    String[] actual = {type};
	    Map<String,String[]> actualParams = new HashMap<String, String[]>();
	    actualParams.put("type", actual);
	    throw new UnsatisfiedServletRequestParameterException(paramConditions,actualParams);
	}
	if(!shorter.equals("first") && !shorter.equals("last")){
	    String[] paramConditions = {"first","last"};
	    String[] actual = {shorter};
	    Map<String,String[]> actualParams = new HashMap<String, String[]>();
	    actualParams.put("shorter", actual);
	    throw new UnsatisfiedServletRequestParameterException(paramConditions,actualParams);
	}
	if(!debug.equals("true") && !debug.equals("false")){
	    String[] paramConditions = {"true","false"};
	    String[] actual = {debug};
	    Map<String,String[]> actualParams = new HashMap<String, String[]>();
	    actualParams.put("debug", actual);
	    throw new UnsatisfiedServletRequestParameterException(paramConditions,actualParams);
	}

	if(markUrl.equals("")){
	    if(lang.equals("en")){
		markUrl = "http://dbpedia.org/resource/";
	    }
	    else if(lang.equals("es")){
		markUrl = "http://es.dbpedia.org/resource/";
	    }
	}


	TnCIndex tncIndex = new TnCIndex(host, port, index);
	ResultIndex result = tncIndex.index(docsDir, lang, type, markSource, markUrl, Integer.parseInt(nwords), shorter, index, debug);

	return result;
    }


    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
