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

import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import net.sourceforge.argparse4j.inf.Subparsers;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;



public class CLI {

    private Namespace parsedArguments = null;

    private ArgumentParser argParser = ArgumentParsers.newArgumentParser(
      "TextnConceptsRetrieval.jar").description("TextnConceptsRetrieval is a retrieval system based on SOLR search platform. It allows to index raw text, as well as concepts. When querying, it is possible to use raw text or concepts.\n");
    private Subparsers subParsers = argParser.addSubparsers().help("sub-command help");
    private Subparser indexParser;
    private Subparser queryParser;


    public CLI() {
	indexParser = subParsers.addParser("index").help("Index CLI");
	loadIndexParameters();
	queryParser = subParsers.addParser("query").help("Query CLI");
	loadQueryParameters();
    }

    
    public static void main(final String[] args){
	CLI cmdLine = new CLI();
	cmdLine.parseCLI(args);
    }


    public final void parseCLI(final String[] args){
	try {
	    parsedArguments = argParser.parseArgs(args);
	    System.err.println("CLI options: " + parsedArguments);
	    if (args[0].equals("index")) {
		index();
	    } else if (args[0].equals("query")) {
		query();
	    }
	} catch (ArgumentParserException e) {
	    argParser.handleError(e);
	    System.out.println("Run java -jar TextnConceptsRetrieval.jar (index|query) -help for details");
	    System.exit(1);
	}
    }


    public final void index() {
	String docList = parsedArguments.getString("docs");
	String type = parsedArguments.getString("type");
	String expdir = parsedArguments.getString("expdir");
	int nwords = Integer.parseInt(parsedArguments.getString("nwords"));
	int nexp = Integer.parseInt(parsedArguments.getString("nexp"));
	String shorter = parsedArguments.getString("shorter");
	String host = parsedArguments.getString("host");
	String port = parsedArguments.getString("port");
	String index = parsedArguments.getString("index");
	boolean commit = parsedArguments.getBoolean("commit");

	TnCIndex tncIndex = new TnCIndex(host, port, index);
	tncIndex.index(docList, type, expdir, nwords, nexp, shorter, index, commit);
    }

    
    public final void query(){
	String queryList = parsedArguments.getString("queries");
	String type = parsedArguments.getString("type");
	String markSource = parsedArguments.getString("mark");
	String markUrl = parsedArguments.getString("url");
	String expdir = parsedArguments.getString("expdir");
	int nwords = Integer.parseInt(parsedArguments.getString("nwords"));
	int nexp = Integer.parseInt(parsedArguments.getString("nexp"));
	String shorter = parsedArguments.getString("shorter");
	String host = parsedArguments.getString("host");
	String port = parsedArguments.getString("port");
	String index = parsedArguments.getString("index");
	boolean debug = parsedArguments.getBoolean("debug");

	TnCQuery tncQuery = new TnCQuery(host, port, index);
	tncQuery.runQueries(queryList, index, type, markSource, markUrl, expdir, nwords, nexp, shorter, debug);
    }


    private void loadIndexParameters() {
	indexParser.addArgument("-d", "--docs")
	    .required(true)
	    .help("File with a document list to be indexed\n");
	indexParser.addArgument("-t", "--type")
	    .required(false)
	    .choices("text", "concepts", "expansion")
	    .setDefault("text")
	    .help("Type of the index to be created: 'text' (only raw text), 'concepts' (text + concepts) or 'expansion' (text + concepts + expansion); it defaults to 'text'.\n");
	indexParser.addArgument("-e", "--expdir")
	    .required(false)
	    .help("Path to a directory containing expansion files (PPV files). Required if 'type' is specified as 'expansion'\n");
	indexParser.addArgument("--nwords")
	    .required(false)
	    .setDefault("-1")
	    .help("Number of words to index; it defaults to '-1' (index all words). Works only when input documents are formatted in NAF.\n");
	indexParser.addArgument("--nexp")
	    .required(false)
	    .setDefault("-1")
	    .help("Number of expansion concepts to index; it dedaults to '0'. Works only when 'type' is specified as 'expansion'.\n");
	indexParser.addArgument("-s", "--shorter")
	    .required(false)
	    .choices("first", "last")
	    .setDefault("first")
	    .help("Makes the document to be indexed shorter. FIRST or LAST nwords of each document will be indexed; defaults to 'first'. Works only when input documents are formatted in NAF.\n");
	indexParser.addArgument("--commit")
	    .required(false)
	    .choices(true,false)
	    .type(Boolean.class)
	    .setDefault(true)
	    .help("If true, after 5000 documents are indexed a splicit commit will be performed; if false, commits will be performed according to the autocommit configuration in solrconfig.xml. It defaults to true.\n");
	indexParser.addArgument("--host")
	    .required(false)
	    .setDefault("localhost")
	    .help("Hostname where Solr server is running; it defaults to 'localhost'.\n");
	indexParser.addArgument("--port")
	    .required(false)
	    .setDefault("8983")
	    .help("Port number where Solr server is running; it defaults to '8983'.\n");
	indexParser.addArgument("-i","--index")
	    .required(true)
	    .help("Solr index name.\n");
    }


    private void loadQueryParameters() {
	queryParser.addArgument("-q", "--queries")
	    .required(true)
	    .help("File with a document list to be queried\n");
	queryParser.addArgument("-t", "--type")
	    .required(false)
	    .choices("text","concepts","expansion","all")
	    .setDefault("text")
	    .help("Type of the query: 'text' (only raw text), 'concepts' (only concepts), 'expansion' (concepts + expansion) or 'all' (text + concepts + expansion); it defaults to 'text'. If 'concepts', 'expansion' or 'all' selected, input queries must be formatted in NAF. If 'expansion' selected, 'expdir' path must be specified\n");
	queryParser.addArgument("-m", "--mark")
	    .required(false)
	    .setDefault("DBpedia_spotlight")
	    .help("Value of the 'source' attribute of <mark> elements in NAF documents to be used as concepts; it defaults to 'DBpedia_spotlight'. Required if 'type' specified as 'concepts', 'expansion' or 'all'.\n");
	queryParser.addArgument("-u", "--url")
	    .required(false)
	    .setDefault("http://dbpedia.org/resource/")
	    .help("Base of the URL in 'reference' attribute of the <externalRef> element in NAF documents; it defaults to 'http://dbpedia.org/resource/' (for example, for http://dbpedia.org/resource/Time_standard). Required if 'type' specified as 'concepts', 'expansion' or 'all'.\n");
	queryParser.addArgument("-e", "--expdir")
	    .required(false)
	    .help("Path to a directory containing expansion files (PPV files). Required if 'type¡ is specified as 'expansion' or 'all'\n");
	queryParser.addArgument("--nwords")
	    .required(false)
	    .setDefault("-1")
	    .help("Number of words to use for querying; it defaults to '-1' (use all words). Works only when input documents are formatted in NAF.\n");
	queryParser.addArgument("--nexp")
	    .required(false)
	    .setDefault("-1")
	    .help("Number of expansion concepts to use for querying; it dedaults to '0'. Works only when 'type' is specified as 'expansion' or 'all'.\n");
	queryParser.addArgument("-s", "--shorter")
	    .required(false)
	    .choices("first", "last")
	    .setDefault("first")
	    .help("Makes the query shorter. FIRST or LAST nwords of each query will be used for querying; defaults to 'first'. Works only when input documents are formatted in NAF.\n");
	queryParser.addArgument("--host")
	    .required(false)
	    .setDefault("localhost")
	    .help("Hostname name where Solr server is running; it defaults to 'localhost'.\n");
	queryParser.addArgument("--port")
	    .required(false)
	    .setDefault("8983")
	    .help("Port number where Solr server is running; defaults to '8983'.\n");
	queryParser.addArgument("-i","--index")
	    .required(true)
	    .help("Solr index name.\n");
	queryParser.addArgument("--debug")
	    .choices(true,false)
	    .type(Boolean.class)
	    .required(false)
	    .setDefault(false)
	    .help("Enable if you want to debug the query.\n");
    }

}