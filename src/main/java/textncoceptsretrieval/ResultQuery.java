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

import java.util.List;


public class ResultQuery {

    private final List<ResultDoc> resultQuery;
    private final int numFound;
    private final String time;
    private final String warning;


    public ResultQuery(List<ResultDoc> resultQuery, int numFound, String time, String warning) {
	this.resultQuery = resultQuery;
	this.numFound = numFound;
        this.time = time;
	this.warning = warning;
    }

    public List<ResultDoc> getResultQuery() {
	return resultQuery;
    }

    public int getNumfound() {
	return numFound;
    }

    public String getTime() {
        return time;
    }

    public String getWarning() {
	return warning;
    }

}
