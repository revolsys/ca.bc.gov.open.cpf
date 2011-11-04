<div>
  <h1>Java Client Example 2</h1>

<div class="java"><code class="java"><span class="java2">/*<br />
 Copyright 2009 Revolution Systems Inc.<br />
<br />
 Licensed under the Apache License, Version 2.0 (the &#34;License&#34;);<br />
 you may not use this file except in compliance with the License.<br />
 You may obtain a copy of the License at<br />
<br />
&#xA0;&#xA0;&#xA0;&#xA0; http://www.apache.org/licenses/LICENSE-2.0<br />
 <br />
 Unless required by applicable law or agreed to in writing, software<br />
 distributed under the License is distributed on an &#34;AS IS&#34; BASIS,<br />
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.<br />
 See the License for the specific language governing permissions and<br />
 limitations under the License.<br />
 <br />
 $Author: chris.ogrady@revolsys.com $<br />
 $Date: 2009-07-07 11:27:35 -0700 (Tue, 07 Jul 2009) $<br />
 $Revision: 1959 $<br />
*/<br />
</span><span class="java4">package </span><span class="java10">examples;<br />
<br />
</span><span class="java4">import </span><span class="java10">java.io.File;<br />
</span><span class="java4">import </span><span class="java10">java.io.IOException;<br />
</span><span class="java4">import </span><span class="java10">java.util.HashMap;<br />
</span><span class="java4">import </span><span class="java10">java.util.Map;<br />
<br />
</span><span class="java4">import </span><span class="java10">com.revolsys.grid.client.api.BatchJobClient;<br />
</span><span class="java4">import </span><span class="java10">com.revolsys.io.PrintUtil;<br />
<br />
</span><span class="java14">/**<br />
 * Submit a job, using a local data file for batch job requests data.<br />
 * Wait for the job to complete, timing out after 1 hour.&#xA0; Get and display the job status.<br />
 * Close the job.<br />
 */<br />
</span><span class="java4">public class </span><span class="java10">WsClientExample2 </span><span class="java8">{<br />
<br />
&#xA0; </span><span class="java4">private </span><span class="java10">BatchJobClient batchJobClient;<br />
<br />
&#xA0; </span><span class="java4">public static </span><span class="java9">void </span><span class="java10">main</span><span class="java8">(</span><span class="java4">final </span><span class="java10">String</span><span class="java8">[] </span><span class="java10">args</span><span class="java8">) {<br />
&#xA0;&#xA0;&#xA0; </span><span class="java10">WsClientExample2 wsClient = </span><span class="java4">new </span><span class="java10">WsClientExample2</span><span class="java8">()</span><span class="java10">;<br />
&#xA0;&#xA0;&#xA0; wsClient.execute</span><span class="java8">()</span><span class="java10">;<br />
&#xA0; </span><span class="java8">}<br />
<br />
&#xA0; </span><span class="java4">public </span><span class="java9">void </span><span class="java10">execute</span><span class="java8">() {<br />
&#xA0;&#xA0;&#xA0; </span><span class="java10">String wsBaseUrl = </span><span class="java5">&#34;http://localhost:8080/ws/&#34;</span><span class="java10">;<br />
&#xA0;&#xA0;&#xA0; String oAuthConsumerKey = </span><span class="java5">&#34;ae4a7382-85c0-46e0-986f-def6f7b9b12a&#34;</span><span class="java10">;<br />
&#xA0;&#xA0;&#xA0; String oAuthConsumerSecret = </span><span class="java5">&#34;fd6417b5-722e-4264-9f3d-3effe819616b&#34;</span><span class="java10">;<br />
&#xA0;&#xA0;&#xA0; batchJobClient = </span><span class="java4">new </span><span class="java10">BatchJobClient</span><span class="java8">(</span><span class="java10">wsBaseUrl, oAuthConsumerKey,<br />
&#xA0;&#xA0;&#xA0;&#xA0;&#xA0; oAuthConsumerSecret</span><span class="java8">)</span><span class="java10">;<br />
<br />
&#xA0;&#xA0;&#xA0; System.out.println</span><span class="java8">(</span><span class="java5">&#34;\nExample 2\n\nsubmitting batch job by file&#34;</span><span class="java8">)</span><span class="java10">;<br />
&#xA0;&#xA0;&#xA0; <br />
&#xA0;&#xA0;&#xA0; Map&lt;String, String&gt; jobStatus;<br />
&#xA0;&#xA0;&#xA0; </span><span class="java4">try </span><span class="java8">{<br />
&#xA0;&#xA0;&#xA0;&#xA0;&#xA0; </span><span class="java10">jobStatus = submitBatchJobByFile</span><span class="java8">()</span><span class="java10">;<br />
<br />
&#xA0;&#xA0;&#xA0;&#xA0;&#xA0; </span><span class="java4">if </span><span class="java8">(</span><span class="java10">jobStatus == </span><span class="java4">null</span><span class="java8">) {<br />
&#xA0;&#xA0;&#xA0;&#xA0;&#xA0;&#xA0;&#xA0; </span><span class="java10">System.out.println</span><span class="java8">(</span><span class="java5">&#34;\njob submission failed&#34;</span><span class="java8">)</span><span class="java10">;<br />
&#xA0;&#xA0;&#xA0;&#xA0;&#xA0; </span><span class="java8">} </span><span class="java4">else </span><span class="java8">{<br />
&#xA0;&#xA0;&#xA0;&#xA0;&#xA0;&#xA0;&#xA0; </span><span class="java4">if </span><span class="java8">(</span><span class="java10">!</span><span class="java5">&#34;resultGenerated&#34;</span><span class="java10">.equals</span><span class="java8">(</span><span class="java10">jobStatus.get</span><span class="java8">(</span><span class="java5">&#34;jobStatus&#34;</span><span class="java8">))) {<br />
&#xA0;&#xA0;&#xA0;&#xA0;&#xA0;&#xA0;&#xA0;&#xA0;&#xA0; </span><span class="java10">System.out.println</span><span class="java8">(</span><span class="java5">&#34;\njob has been submitted&#34;</span><span class="java8">)</span><span class="java10">;<br />
&#xA0;&#xA0;&#xA0;&#xA0;&#xA0;&#xA0;&#xA0; </span><span class="java8">}<br />
&#xA0;&#xA0;&#xA0;&#xA0;&#xA0;&#xA0;&#xA0; </span><span class="java10">System.out.println</span><span class="java8">(</span><span class="java5">&#34;\njob status&#34;</span><span class="java8">)</span><span class="java10">;<br />
&#xA0;&#xA0;&#xA0;&#xA0;&#xA0;&#xA0;&#xA0; PrintUtil.print</span><span class="java8">(</span><span class="java10">jobStatus, System.out</span><span class="java8">)</span><span class="java10">;<br />
&#xA0;&#xA0;&#xA0;&#xA0;&#xA0; </span><span class="java8">}<br />
&#xA0;&#xA0;&#xA0; } </span><span class="java4">catch </span><span class="java8">(</span><span class="java10">IOException e</span><span class="java8">) {<br />
&#xA0;&#xA0;&#xA0;&#xA0;&#xA0; </span><span class="java10">e.printStackTrace</span><span class="java8">()</span><span class="java10">;<br />
&#xA0;&#xA0;&#xA0; </span><span class="java8">} </span><span class="java4">catch </span><span class="java8">(</span><span class="java10">RuntimeException e</span><span class="java8">) {<br />
&#xA0;&#xA0;&#xA0;&#xA0;&#xA0; </span><span class="java10">e.printStackTrace</span><span class="java8">()</span><span class="java10">;<br />
&#xA0;&#xA0;&#xA0; </span><span class="java8">}<br />
&#xA0;&#xA0;&#xA0; <br />
&#xA0;&#xA0;&#xA0; </span><span class="java10">disconnect</span><span class="java8">()</span><span class="java10">;<br />
&#xA0; </span><span class="java8">}<br />
<br />
&#xA0; </span><span class="java4">private </span><span class="java10">Map&lt;String, String&gt; submitBatchJobByFile</span><span class="java8">() {<br />
&#xA0;&#xA0;&#xA0; </span><span class="java10">String businessApplicationName = </span><span class="java5">&#34;mapTileByLocation&#34;</span><span class="java10">;<br />
&#xA0;&#xA0;&#xA0; String businessApplicationVersion = </span><span class="java5">&#34;1.0.0&#34;</span><span class="java10">;<br />
&#xA0;&#xA0;&#xA0; </span><span class="java9">int </span><span class="java10">numberOfRequests = </span><span class="java7">192</span><span class="java10">;<br />
&#xA0;&#xA0;&#xA0; String inputDataFile = </span><span class="java5">&#34;C://tmp//NTS-250000-by-location.csv&#34;</span><span class="java10">;<br />
&#xA0;&#xA0;&#xA0; File inputFile = </span><span class="java4">new </span><span class="java10">File</span><span class="java8">(</span><span class="java10">inputDataFile</span><span class="java8">)</span><span class="java10">;<br />
&#xA0;&#xA0;&#xA0; String inputDataContentType = </span><span class="java5">&#34;text/csv&#34;</span><span class="java10">;<br />
&#xA0;&#xA0;&#xA0; String resultContentType = </span><span class="java5">&#34;text/csv&#34;</span><span class="java10">;<br />
&#xA0;&#xA0;&#xA0; Map&lt;String, String&gt; parameters = </span><span class="java4">new </span><span class="java10">HashMap&lt;String, String&gt;</span><span class="java8">()</span><span class="java10">;<br />
&#xA0;&#xA0;&#xA0; parameters.put</span><span class="java8">(</span><span class="java5">&#34;mapGridName&#34;</span><span class="java10">, </span><span class="java5">&#34;NTS 1:250 000&#34;</span><span class="java8">)</span><span class="java10">;<br />
<br />
&#xA0;&#xA0;&#xA0; </span><span class="java9">boolean </span><span class="java10">waitForResponse = </span><span class="java4">true</span><span class="java10">;<br />
&#xA0;&#xA0;&#xA0; </span><span class="java9">int </span><span class="java10">maxWaitTime = </span><span class="java7">3600000</span><span class="java10">;<br />
&#xA0;&#xA0;&#xA0; Map&lt;String, String&gt; jobStatus = </span><span class="java4">null</span><span class="java10">;<br />
<br />
&#xA0;&#xA0;&#xA0; </span><span class="java4">try </span><span class="java8">{<br />
&#xA0;&#xA0;&#xA0;&#xA0;&#xA0; </span><span class="java10">jobStatus = batchJobClient.submitJob</span><span class="java8">(</span><span class="java10">businessApplicationName,<br />
&#xA0;&#xA0;&#xA0;&#xA0;&#xA0;&#xA0;&#xA0; businessApplicationVersion, numberOfRequests, inputFile, inputDataContentType,<br />
&#xA0;&#xA0;&#xA0;&#xA0;&#xA0;&#xA0;&#xA0; resultContentType, parameters, waitForResponse, maxWaitTime</span><span class="java8">)</span><span class="java10">;<br />
&#xA0;&#xA0;&#xA0; </span><span class="java8">} </span><span class="java4">catch </span><span class="java8">(</span><span class="java10">IOException e</span><span class="java8">) {<br />
&#xA0;&#xA0;&#xA0;&#xA0;&#xA0; </span><span class="java10">e.printStackTrace</span><span class="java8">()</span><span class="java10">;<br />
&#xA0;&#xA0;&#xA0; </span><span class="java8">} </span><span class="java4">catch </span><span class="java8">(</span><span class="java10">RuntimeException e</span><span class="java8">) {<br />
&#xA0;&#xA0;&#xA0;&#xA0;&#xA0; </span><span class="java10">e.printStackTrace</span><span class="java8">()</span><span class="java10">;<br />
&#xA0;&#xA0;&#xA0; </span><span class="java8">} </span><span class="java4">catch </span><span class="java8">(</span><span class="java10">InterruptedException e</span><span class="java8">) {<br />
&#xA0;&#xA0;&#xA0;&#xA0;&#xA0; </span><span class="java10">e.printStackTrace</span><span class="java8">()</span><span class="java10">;<br />
&#xA0;&#xA0;&#xA0; </span><span class="java8">}<br />
&#xA0;&#xA0;&#xA0; </span><span class="java4">return </span><span class="java10">jobStatus;<br />
&#xA0; </span><span class="java8">}<br />
<br />
&#xA0; </span><span class="java4">private </span><span class="java9">void </span><span class="java10">disconnect</span><span class="java8">() {<br />
&#xA0;&#xA0;&#xA0; </span><span class="java10">batchJobClient.closeConnection</span><span class="java8">()</span><span class="java10">;<br />
&#xA0;&#xA0;&#xA0; batchJobClient = </span><span class="java4">null</span><span class="java10">;<br />
&#xA0; </span><span class="java8">}<br />
<br />
}</span></code></div>


<br /><br />
<a href="../sampleCode/WsClientExample2.java" class="button" target="_blank">download code</a>
<br /><br />
