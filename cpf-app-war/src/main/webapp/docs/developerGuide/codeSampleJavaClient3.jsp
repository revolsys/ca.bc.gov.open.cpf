<div>
  <h1>Java Client Example 3</h1>

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
<br />
</span><span class="java4">import </span><span class="java10">com.revolsys.grid.client.api.BatchJobClient;<br />
</span><span class="java4">import </span><span class="java10">com.revolsys.io.PrintUtil;<br />
<br />
<br />
</span><span class="java14">/**<br />
 * List all the business application mapTileByLocation jobs for a specific user. <br />
 */<br />
</span><span class="java4">public class </span><span class="java10">WsClientExample3 </span><span class="java8">{<br />
<br />
&#xA0; </span><span class="java4">private </span><span class="java10">BatchJobClient batchJobClient;<br />
<br />
&#xA0; </span><span class="java4">public static </span><span class="java9">void </span><span class="java10">main</span><span class="java8">(</span><span class="java4">final </span><span class="java10">String</span><span class="java8">[] </span><span class="java10">args</span><span class="java8">) {<br />
&#xA0;&#xA0;&#xA0; </span><span class="java10">WsClientExample3 wsClient = </span><span class="java4">new </span><span class="java10">WsClientExample3</span><span class="java8">()</span><span class="java10">;<br />
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
&#xA0;&#xA0;&#xA0; System.out.println</span><span class="java8">(</span><span class="java5">&#34;\nExample 3\n\ngetting list of users jobs&#34;</span><span class="java8">)</span><span class="java10">;<br />
&#xA0;&#xA0;&#xA0; List&lt;String&gt; jobList = </span><span class="java4">null</span><span class="java10">;<br />
&#xA0;&#xA0;&#xA0; </span><span class="java4">try </span><span class="java8">{<br />
&#xA0;&#xA0;&#xA0;&#xA0;&#xA0; </span><span class="java10">jobList = batchJobClient.getUserJobs</span><span class="java8">(</span><span class="java5">&#34;mapTileByLocation&#34;</span><span class="java8">)</span><span class="java10">;<br />
&#xA0;&#xA0;&#xA0;&#xA0;&#xA0; PrintUtil.print</span><span class="java8">(</span><span class="java10">jobList, System.out</span><span class="java8">)</span><span class="java10">;<br />
&#xA0;&#xA0;&#xA0; </span><span class="java8">} </span><span class="java4">catch </span><span class="java8">(</span><span class="java10">IllegalStateException e</span><span class="java8">) {<br />
&#xA0;&#xA0;&#xA0;&#xA0;&#xA0; </span><span class="java10">e.printStackTrace</span><span class="java8">()</span><span class="java10">;<br />
&#xA0;&#xA0;&#xA0; </span><span class="java8">} </span><span class="java4">catch </span><span class="java8">(</span><span class="java10">IOException e</span><span class="java8">) {<br />
&#xA0;&#xA0;&#xA0;&#xA0;&#xA0; </span><span class="java10">e.printStackTrace</span><span class="java8">()</span><span class="java10">;<br />
&#xA0;&#xA0;&#xA0; </span><span class="java8">}<br />
&#xA0;&#xA0;&#xA0; <br />
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
<a href="../sampleCode/WsClientExample3.java" class="button" target="_blank">download code</a>
<br /><br />
