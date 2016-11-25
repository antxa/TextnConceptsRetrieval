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
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;


@ApiModel("Concepts Result")
public class ResultConcepts {

    @ApiModelProperty(value = "list of concepts", required = true)
    private final List<String> concepts;
    @ApiModelProperty(value = "time", required = true)
    private final String time;
    @ApiModelProperty(value = "warning messages", required = true)
    private final String warning;


    public ResultConcepts(List<String> concepts, String time, String warning) {
	this.concepts = concepts;
        this.time = time;
	this.warning = warning;
    }

    public List<String> getConcepts() {
	return concepts;
    }

    public String getTime() {
        return time;
    }

    public String getWarning() {
	return warning;
    }

}
