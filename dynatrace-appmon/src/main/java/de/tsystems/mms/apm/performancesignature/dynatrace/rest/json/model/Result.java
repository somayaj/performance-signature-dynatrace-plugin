/*
 * Copyright (c) 2014-2018 T-Systems Multimedia Solutions GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.tsystems.mms.apm.performancesignature.dynatrace.rest.json.model;

import com.google.gson.annotations.SerializedName;
import de.tsystems.mms.apm.performancesignature.ui.util.PerfSigUIUtils;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * Single string result
 */
@ApiModel(description = "Single string result")

public class Result {
    @SerializedName("result")
    private String result;

    /**
     * XmlResult value
     *
     * @return result
     **/
    @ApiModelProperty(required = true, value = "XmlResult value")
    public String getResult() {
        return result;
    }

    @Override
    public String toString() {
        return "class XmlResult {\n" +
                "    result: " + PerfSigUIUtils.toIndentedString(result) + "\n" +
                "}";
    }
}
