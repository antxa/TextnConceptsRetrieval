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

import java.io.IOException;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.client.solrj.*;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.client.solrj.impl.HttpSolrClient;


public class SolrjClient {

    private SolrClient client;

    public SolrjClient(String server, String port, String index){
	String urlString = "http://" + server + ":" + port + "/solr/" + index;
	client = new HttpSolrClient.Builder(urlString).build();
    }

    
    public void index(SolrInputDocument solrDoc){
	try{
	    UpdateResponse response = client.add(solrDoc);
	}catch(SolrServerException e){
	    e.printStackTrace();
	} catch(IOException e){
	    e.printStackTrace();
	}
    }


    public QueryResponse query(SolrQuery query){
	QueryResponse response = null;
	try{
	    response = client.query(query,SolrRequest.METHOD.POST);
	} catch(SolrServerException e){
	    e.printStackTrace();
	} catch(IOException e){
	    e.printStackTrace();
	}

	return response;
    }


    public void commit(){
	try{
	    UpdateResponse  response = client.commit();
	} catch(SolrServerException e){
	    e.printStackTrace();
	} catch(IOException e){
	    e.printStackTrace();
	}
    }


    public void close(){
	try{
	    client.close();
	} catch(IOException e){
	    e.printStackTrace();
	}
    }

}