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

package de.tsystems.mms.apm.performancesignature.viewer.rest.model;

import de.tsystems.mms.apm.performancesignature.dynatrace.model.DashboardReport;

import java.util.ArrayList;
import java.util.List;

public class RootElement {
    private List<DashboardReport> dashboardReports;

    public List<DashboardReport> getDashboardReports() {
        if (dashboardReports == null) {
            dashboardReports = new ArrayList<>();
        }
        return dashboardReports;
    }
}