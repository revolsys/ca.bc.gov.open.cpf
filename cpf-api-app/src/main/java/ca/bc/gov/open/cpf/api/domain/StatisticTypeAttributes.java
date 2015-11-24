/*
 * Copyright © 2008-2015, Province of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ca.bc.gov.open.cpf.api.domain;

import java.util.HashMap;
import java.util.Map;

public class StatisticTypeAttributes {
  private final static Map<String, String> units = new HashMap<String, String>() {
    private static final long serialVersionUID = 1L;

    {
      put("default", "Time (milliseconds)");
      put("averageCountJobCreatedPerMinute", "Number of Jobs");
      put("averageCountRequestCreatedPerMinute", "Number of Requests");
      put("averageCountRequestProcessedPerMinute", "Number of Requests");
      put("currentCpuLoad", "% CPU Usage");
      put("heapMemoryUsed", "Memory (MB)");
      put("nonHeapMemoryUsed", "Megabytes");
      put("currentActiveJobs", "Number of Jobs");
      put("percentCurrentActiveJobs", "Percent");
      put("percentCurrentWaitingJobs", "Percent");
    }
  };

  private final static Map<String, Double> divisor = new HashMap<String, Double>() {
    private static final long serialVersionUID = 1L;

    {
      put("currentCpuLoad", .01);
      put("heapMemoryUsed", 1000000D);
      put("nonHeapMemoryUsed", 1000000D);
      put("percentCurrentActiveJobs", .01);
      put("percentCurrentWaitingJobs", .01);
    }
  };

  public static Double getDivisor(final String statisticType) {
    if (divisor.containsKey(statisticType)) {
      return divisor.get(statisticType);
    } else {
      return 1D;
    }
  }

  public static String getUnits(final String statisticType) {
    if (units.containsKey(statisticType)) {
      return units.get(statisticType);
    } else {
      return units.get("default");
    }
  }
}
