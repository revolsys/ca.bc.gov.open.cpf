<div>
  <h1>Java Client Example 4</h1>

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
</span><span class="java4">import </span><span class="java10">java.io.IOException;<br />
</span><span class="java4">import </span><span class="java10">java.util.List;<br />
</span><span class="java4">import </span><span class="java10">java.util.Map;<br />
<br />
</span><span class="java4">import </span><span class="java10">com.revolsys.grid.client.api.BatchJobClient;<br />
</span><span class="java4">import </span><span class="java10">com.revolsys.io.MapReader;<br />
</span><span class="java4">import </span><span class="java10">com.revolsys.io.PrintUtil;<br />
<br />
</span><span class="java14">/**<br />
 * Get the job status of an existing successfully processed job. <br />
 * List the jobs result files. <br />
 * Get a map reader of the results. <br />
 * Display the results.<br />
 *<br />
 */<br />
</span><span class="java4">public class </span><span class="java10">WsClientExample4 </span><span class="java8">{<br />
<br />
&#xA0; </span><span class="java4">private </span><span class="java10">BatchJobClient batchJobClient;<br />
<br />
&#xA0; </span><span class="java4">public static </span><span class="java9">void </span><span class="java10">main</span><span class="java8">(</span><span class="java4">final </span><span class="java10">String</span><span class="java8">[] </span><span class="java10">args</span><span class="java8">) {<br />
&#xA0;&#xA0;&#xA0; </span><span class="java10">WsClientExample4 wsClient = </span><span class="java4">new </span><span class="java10">WsClientExample4</span><span class="java8">()</span><span class="java10">;<br />
&#xA0;&#xA0;&#xA0; wsClient.execute</span><span class="java8">()</span><span class="java10">;<br />
&#xA0; </span><span class="java8">}<br />
<br />
&#xA0; </span><span class="java4">public </span><span class="java9">void </span><span class="java10">execute</span><span class="java8">() {<br />
&#xA0;&#xA0;&#xA0; </span><span class="java10">String wsBaseUrl = </span><span class="java5">&#34;http://localhost:8080/ws/&#34;</span><span class="java10">;<br />
&#xA0;&#xA0;&#xA0; String oAuthConsumerKey = </span><span class="java5">&#34;ae4a7382-85c0-46e0-986f-def6f7b9b12a&#34;</span><span class="java10">;<br />
&#xA0;&#xA0;&#xA0; String oAuthConsumerSecret = </span><span class="java5">&#34;fd6417b5-722e-4264-9f3d-3effe819616b&#34;</span><span class="java10">;<br />
&#xA0;&#xA0;&#xA0; </span><span class="java9">int </span><span class="java10">jobId = </span><span class="java7">500</span><span class="java10">;<br />
&#xA0;&#xA0;&#xA0; batchJobClient = </span><span class="java4">new </span><span class="java10">BatchJobClient</span><span class="java8">(</span><span class="java10">wsBaseUrl, oAuthConsumerKey,<br />
&#xA0;&#xA0;&#xA0;&#xA0;&#xA0; oAuthConsumerSecret</span><span class="java8">)</span><span class="java10">;<br />
<br />
&#xA0;&#xA0;&#xA0; System.out.println</span><span class="java8">(</span><span class="java5">&#34;\nExample 4\n\ngetting job status&#34;</span><span class="java8">)</span><span class="java10">;<br />
&#xA0;&#xA0;&#xA0; String jobUrl = wsBaseUrl + </span><span class="java5">&#34;users/&#34; </span><span class="java10">+ oAuthConsumerKey + </span><span class="java5">&#34;/jobs/&#34; </span><span class="java10">+ jobId;<br />
&#xA0;&#xA0;&#xA0; Map&lt;String, String&gt; jobStatus;<br />
&#xA0;&#xA0;&#xA0; </span><span class="java4">try </span><span class="java8">{<br />
&#xA0;&#xA0;&#xA0;&#xA0;&#xA0; </span><span class="java10">System.out.println</span><span class="java8">(</span><span class="java5">&#34;\njob status&#34;</span><span class="java8">)</span><span class="java10">;<br />
&#xA0;&#xA0;&#xA0;&#xA0;&#xA0; jobStatus = batchJobClient.getJobStatus</span><span class="java8">(</span><span class="java10">jobUrl</span><span class="java8">)</span><span class="java10">;<br />
&#xA0;&#xA0;&#xA0;&#xA0;&#xA0; PrintUtil.print</span><span class="java8">(</span><span class="java10">jobStatus, System.out</span><span class="java8">)</span><span class="java10">;<br />
<br />
&#xA0;&#xA0;&#xA0;&#xA0;&#xA0; System.out.println</span><span class="java8">(</span><span class="java5">&#34;\nresult files&#34;</span><span class="java8">)</span><span class="java10">;<br />
&#xA0;&#xA0;&#xA0;&#xA0;&#xA0; List&lt;Map&lt;String, String&gt;&gt; resultsFileList = batchJobClient.getJobResultsList</span><span class="java8">(</span><span class="java10">jobStatus.get</span><span class="java8">(</span><span class="java5">&#34;resultsUrl&#34;</span><span class="java8">))</span><span class="java10">;<br />
&#xA0;&#xA0;&#xA0;&#xA0;&#xA0; </span><span class="java4">for </span><span class="java8">(</span><span class="java10">Map&lt;String, String&gt; resultsFile : resultsFileList</span><span class="java8">) {<br />
&#xA0;&#xA0;&#xA0;&#xA0;&#xA0;&#xA0;&#xA0; </span><span class="java10">PrintUtil.print</span><span class="java8">(</span><span class="java10">resultsFile, System.out</span><span class="java8">)</span><span class="java10">;<br />
&#xA0;&#xA0;&#xA0;&#xA0;&#xA0;&#xA0;&#xA0; System.out.println</span><span class="java8">(</span><span class="java5">&#34;\nresult file &#34; </span><span class="java10">+ resultsFile.get</span><span class="java8">(</span><span class="java5">&#34;resourceUri&#34;</span><span class="java8">))</span><span class="java10">;<br />
&#xA0;&#xA0;&#xA0;&#xA0;&#xA0;&#xA0;&#xA0; MapReader responseMapReader = batchJobClient.getResponseMapReader</span><span class="java8">(</span><span class="java10">resultsFile.get</span><span class="java8">(</span><span class="java5">&#34;resourceUri&#34;</span><span class="java8">))</span><span class="java10">;<br />
&#xA0;&#xA0;&#xA0;&#xA0;&#xA0;&#xA0;&#xA0; batchJobClient.printMapReader</span><span class="java8">(</span><span class="java10">responseMapReader</span><span class="java8">)</span><span class="java10">;<br />
&#xA0;&#xA0;&#xA0;&#xA0;&#xA0; </span><span class="java8">}<br />
&#xA0;&#xA0;&#xA0; } </span><span class="java4">catch </span><span class="java8">(</span><span class="java10">RuntimeException e</span><span class="java8">) {<br />
&#xA0;&#xA0;&#xA0;&#xA0;&#xA0; </span><span class="java10">e.printStackTrace</span><span class="java8">()</span><span class="java10">;<br />
&#xA0;&#xA0;&#xA0; </span><span class="java8">} </span><span class="java4">catch </span><span class="java8">(</span><span class="java10">IOException e</span><span class="java8">) {<br />
&#xA0;&#xA0;&#xA0;&#xA0;&#xA0; </span><span class="java10">e.printStackTrace</span><span class="java8">()</span><span class="java10">;<br />
&#xA0;&#xA0;&#xA0; </span><span class="java8">}<br />
<br />
&#xA0;&#xA0;&#xA0; </span><span class="java10">disconnect</span><span class="java8">()</span><span class="java10">;<br />
&#xA0; </span><span class="java8">}<br />
<br />
&#xA0; </span><span class="java4">private </span><span class="java9">void </span><span class="java10">disconnect</span><span class="java8">() {<br />
&#xA0;&#xA0;&#xA0; </span><span class="java10">batchJobClient.closeConnection</span><span class="java8">()</span><span class="java10">;<br />
&#xA0;&#xA0;&#xA0; batchJobClient = </span><span class="java4">null</span><span class="java10">;<br />
&#xA0; </span><span class="java8">}<br />
<br />
}</span></code></div>


<br /><br />
<a href="../sampleCode/WsClientExample4.java" class="button" target="_blank">download code</a>
<br /><br />
