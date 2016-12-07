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

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import org.springframework.http.MediaType;
import org.springframework.boot.SpringApplication;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.UnsatisfiedServletRequestParameterException;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;
import springfox.documentation.service.Contact;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.ResponseHeader;
import io.swagger.annotations.Api;
import java.util.List;
import java.util.Map;
import java.util.HashMap;


@SpringBootApplication
@EnableSwagger2
@RestController
@Api(description = "REST requests")
public class Application {

    @ApiOperation(value = "Submit a query", notes = "Given a query file, retrieves a list of documents in JSON format", response = ResultQuery.class)
    @RequestMapping(value = "/query", method = RequestMethod.POST, produces = "application/json" )
    @ApiImplicitParams({
	    @ApiImplicitParam(name = "qfile", value = "Text file sent as body parameter. The content of the file (text) will be used as a query", required = true, dataType = "MultipartFile", paramType = "body"),
		@ApiImplicitParam(name = "lang", value = "Language of the text in qfile", allowableValues = "en, es", required = true, dataType = "string", paramType = "query"),
		@ApiImplicitParam(name = "type", value = "Monolingual or crosslingual retrieval", allowableValues = "mono, cross", required = true, dataType = "string", paramType = "query"),
		@ApiImplicitParam(name = "mark", value = "Value of the 'source' attribute of 'mark' elements in NAF documents to be used as concepts", required = false, defaultValue = "DBpedia_spotlight", dataType = "string", paramType = "query"),
		@ApiImplicitParam(name = "url", value = "Base of the URL in 'reference' attribute of the English 'externalRef' element in NAF documents", required = false, defaultValue = "http://dbpedia.org/resource/", dataType = "string", paramType = "query"),
		@ApiImplicitParam(name = "nwords", value = "Number of words to use for querying. '-1' for all words", required = false, defaultValue = "-1", dataType = "string", paramType = "query"),
		@ApiImplicitParam(name = "shorter", value = "Makes the query shorter. FIRST or LAST nwords of each query will be used for querying", allowableValues = "first, last", required = false, defaultValue = "first", dataType = "string", paramType = "query"),
		@ApiImplicitParam(name = "ndocs", value = "Number of documents to retrieve", required = false, defaultValue = "10", dataType = "string", paramType = "query"),
		@ApiImplicitParam(name = "host", value = "Hostname where Solr server is running", required = false, defaultValue = "localhost", dataType = "string", paramType = "query"),
		@ApiImplicitParam(name = "port", value = "Port number where Solr server is running", required = false, defaultValue = "8983", dataType = "string", paramType = "query"),
		@ApiImplicitParam(name = "index", value = "Solr index name", required = false, defaultValue = "textnconcepts", dataType = "string", paramType = "query"),
		@ApiImplicitParam(name = "debug", allowableValues = "true, false", required = false, defaultValue = "false", dataType = "string", paramType = "query")})	
		public ResultQuery query(@RequestParam(value="qfile", required=true) MultipartFile qfile,
			     @RequestParam(value="lang", required=true) String lang,
			     @RequestParam(value="type", required=true) String type,
			     @RequestParam(value="mark", required=false, defaultValue="DBpedia_spotlight") String markSource,
			     @RequestParam(value="url", required=false, defaultValue="http://dbpedia.org/resource/") String markUrl,
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
	if(!type.equals("mono") && !type.equals("cross")){
	    String[] paramConditions = {"mono","cross"};
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

	TnCQuery tncQuery = new TnCQuery(host, port, index);
	ResultQuery result = tncQuery.runQuery(qfile, lang, index, type, markSource, markUrl, Integer.parseInt(nwords), shorter, Integer.parseInt(ndocs), debug);
	
	return result;
    }


    @ApiOperation(value = "Index a single document", notes = "Index a single document, passed in the request body", response = ResultIndex.class)
    @RequestMapping(value = "/index", method = RequestMethod.POST, produces = "application/json")
    @ApiImplicitParams({
	    @ApiImplicitParam(name = "doc", value = "Text file sent as body parameter. The content of the file (text) and the concepts derived from it will be indexed", required = true, dataType = "MultipartFile", paramType = "body"),
		@ApiImplicitParam(name = "id", value = "Document id", required = true, dataType = "string", paramType = "query"),
		@ApiImplicitParam(name = "lang", value = "Language of the documents", allowableValues = "en, es", required = true, dataType = "string", paramType = "query"),
		@ApiImplicitParam(name = "mark", value = "Value of the 'source' attribute of 'mark' elements in NAF documents to be used as concepts", required = false, defaultValue = "DBpedia_spotlight", dataType = "string", paramType = "query"),
		@ApiImplicitParam(name = "url", value = "Base of the URL in 'reference' attribute of the English 'externalRef' element in NAF documents", required = false, defaultValue = "http://dbpedia.org/resource/", dataType = "string", paramType = "query"),
		@ApiImplicitParam(name = "nwords", value = "Number of words to index from each document. '-1' for indexing all words", required = false, defaultValue = "-1", dataType = "string", paramType = "query"),
		@ApiImplicitParam(name = "shorter", value = "Makes all the document shorter. It indexes part of each document. FIRST or LAST nwords of each document will be indexed", allowableValues = "first, last", required = false, defaultValue = "first", dataType = "string", paramType = "query"),
		@ApiImplicitParam(name = "host", value = "Hostname where Solr server is running", required = false, defaultValue = "localhost", dataType = "string", paramType = "query"),
		@ApiImplicitParam(name = "port", value = "Port number where Solr server is running", required = false, defaultValue = "8983", dataType = "string", paramType = "query"),
		@ApiImplicitParam(name = "index", value = "Solr index name", required = false, defaultValue = "textnconcepts", dataType = "string", paramType = "query"),
		@ApiImplicitParam(name = "debug", allowableValues = "true, false", required = false, defaultValue = "false", dataType = "string", paramType = "query")})	
		public ResultIndex index(@RequestParam(value="doc", required=true) MultipartFile doc,
					 @RequestParam(value="id", required=true) String id,
					 @RequestParam(value="lang", required=true) String lang,
					 @RequestParam(value="mark", required=false, defaultValue="DBpedia_spotlight") String markSource,
					 @RequestParam(value="url", required=false, defaultValue="http://dbpedia.org/resource/") String markUrl,
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

	TnCIndex tncIndex = new TnCIndex(host, port, index);
	ResultIndex result = tncIndex.index(doc, id, lang, markSource, markUrl, Integer.parseInt(nwords), shorter, index, debug);

	return result;
    }


@ApiOperation(value = "Index documents", notes = "Index documents from a directory", response = ResultIndex.class)
    @RequestMapping(value = "/indexdir", method = RequestMethod.POST, produces = "application/json")
    @ApiImplicitParams({
	    @ApiImplicitParam(name = "docsDir", value = "Path to a directory containing the documents to be indexed. It should be 'docs4indexing' or a subdirectory inside this directory", required = true, defaultValue = "", dataType = "string", paramType = "query"),
		@ApiImplicitParam(name = "lang", value = "Language of the documents", allowableValues = "en, es", required = true, dataType = "string", paramType = "query"),
		@ApiImplicitParam(name = "mark", value = "Value of the 'source' attribute of 'mark' elements in NAF documents to be used as concepts", required = false, defaultValue = "DBpedia_spotlight", dataType = "string", paramType = "query"),
		@ApiImplicitParam(name = "url", value = "Base of the URL in 'reference' attribute of the English 'externalRef' element in NAF documents", required = false, defaultValue = "http://dbpedia.org/resource/", dataType = "string", paramType = "query"),
		@ApiImplicitParam(name = "nwords", value = "Number of words to index from each document. '-1' for indexing all words", required = false, defaultValue = "-1", dataType = "string", paramType = "query"),
		@ApiImplicitParam(name = "shorter", value = "Makes all the document shorter. It indexes part of each document. FIRST or LAST nwords of each document will be indexed", allowableValues = "first, last", required = false, defaultValue = "first", dataType = "string", paramType = "query"),
		@ApiImplicitParam(name = "host", value = "Hostname where Solr server is running", required = false, defaultValue = "localhost", dataType = "string", paramType = "query"),
		@ApiImplicitParam(name = "port", value = "Port number where Solr server is running", required = false, defaultValue = "8983", dataType = "string", paramType = "query"),
		@ApiImplicitParam(name = "index", value = "Solr index name", required = false, defaultValue = "textnconcepts", dataType = "string", paramType = "query"),
		@ApiImplicitParam(name = "debug", allowableValues = "true, false", required = false, defaultValue = "false", dataType = "string", paramType = "query")})	
		public ResultIndex indexdir(@RequestParam(value="docsDir", required=true) String docsDir,
					 @RequestParam(value="lang", required=true) String lang,
					 @RequestParam(value="mark", required=false, defaultValue="DBpedia_spotlight") String markSource,
					 @RequestParam(value="url", required=false, defaultValue="http://dbpedia.org/resource/") String markUrl,
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

	TnCIndex tncIndex = new TnCIndex(host, port, index);
	ResultIndex result = tncIndex.indexdir(docsDir, lang, markSource, markUrl, Integer.parseInt(nwords), shorter, index, debug);

	return result;
    }

   
    @ApiOperation(value = "Get concepts", notes = "Given a document, returns a list of concepts derived from the document", response = ResultConcepts.class)
    @RequestMapping(value = "/concepts", method = RequestMethod.POST, produces = "application/json" )
    @ApiImplicitParams({
	    @ApiImplicitParam(name = "doc", value = "Text file sent as body parameter", required = true, dataType = "MultipartFile", paramType = "body"),
		@ApiImplicitParam(name = "lang", value = "Language of the text of the document", allowableValues = "en, es", required = true, dataType = "string", paramType = "query"),
		@ApiImplicitParam(name = "mark", value = "Value of the 'source' attribute of 'mark' elements in NAF documents", required = false, defaultValue = "DBpedia_spotlight", dataType = "string", paramType = "query"),
		@ApiImplicitParam(name = "url", value = "Base of the URL in 'reference' attribute of the 'externalRef' element in NAF documents", required = false, defaultValue = "http://dbpedia.org/resource/", dataType = "string", paramType = "query"),
		@ApiImplicitParam(name = "debug", allowableValues = "true, false", required = false, defaultValue = "false", dataType = "string", paramType = "query")})	
		public ResultConcepts concepts(@RequestParam(value="doc", required=true) MultipartFile doc,
			     @RequestParam(value="lang", required=true) String lang,
			     @RequestParam(value="mark", required=false, defaultValue="DBpedia_spotlight") String markSource,
			     @RequestParam(value="url", required=false, defaultValue="http://dbpedia.org/resource/") String markUrl,
			     @RequestParam(value="debug", required=false, defaultValue="false") String debug)
	throws UnsatisfiedServletRequestParameterException {

	if(!lang.equals("en") && !lang.equals("es")){
	    String[] paramConditions = {"en","es"};
	    String[] actual = {lang};
	    Map<String,String[]> actualParams = new HashMap<String, String[]>();
	    actualParams.put("lang", actual);
	    throw new UnsatisfiedServletRequestParameterException(paramConditions,actualParams);
	}
	if(!debug.equals("true") && !debug.equals("false")){
	    String[] paramConditions = {"true","false"};
	    String[] actual = {debug};
	    Map<String,String[]> actualParams = new HashMap<String, String[]>();
	    actualParams.put("debug", actual);
	    throw new UnsatisfiedServletRequestParameterException(paramConditions,actualParams);
	}

	TnCConcepts tncConcepts = new TnCConcepts();
	ResultConcepts result = tncConcepts.getConcepts(doc, lang, markSource, markUrl, debug);
	
	return result;
    }



   
    @Bean
    public Docket swaggerSettings() {
        return new Docket(DocumentationType.SWAGGER_2)
	    .select()
	    .apis(RequestHandlerSelectors.any())
	    .paths(Predicates.not(PathSelectors.regex("/error"))) 
	    .build()
	    .pathMapping("/")
	    .apiInfo(apiInfo());
    }


    private ApiInfo apiInfo() {
        ApiInfo apiInfo = new ApiInfo(
            "TextnConceptsRetrieval REST API",
            "This is a REST API for retrieving documents, based on text and concepts.",
            "",
            "",
            new Contact("Arantxa Otegi", "", "arantza.otegi@ehu.eus"),
            "GNU General Public License",
            "http://www.gnu.org/licenses"
        );
        return apiInfo;
    }


    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
