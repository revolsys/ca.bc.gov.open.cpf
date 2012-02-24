<div>
  <h1>Plug-in Example</h1>
  <div class="java"><pre class="java"><span class="java4">package </span><span class="java10">ca.bc.gov.open.cpf.plugin.PluginExample1;<br />
  <br />
  </span><span class="java4">import </span><span class="java10">ca.bc.gov.open.cpf.plugin.api.AllowedValues;<br />
  </span><span class="java4">import </span><span class="java10">ca.bc.gov.open.cpf.plugin.api.BusinessApplicationPlug-in;<br />
  </span><span class="java4">import </span><span class="java10">ca.bc.gov.open.cpf.plugin.api.JobParameter;<br />
  </span><span class="java4">import </span><span class="java10">ca.bc.gov.open.cpf.plugin.api.RequestParameter;<br />
  </span><span class="java4">import </span><span class="java10">ca.bc.gov.open.cpf.plugin.api.Required;<br />
  </span><span class="java4">import </span><span class="java10">ca.bc.gov.open.cpf.plugin.api.ResponseField;<br />
  <br />
  </span><span class="java16">@BusinessApplicationPlugin</span><span class="java8">(</span><span class="java10">name = </span><span class="java5">&#34;PluginExample1&#34;</span><span class="java10">, version = </span><span class="java5">&#34;1.0.0&#34;</span><span class="java8">)<br />
  </span><span class="java4">public class </span><span class="java10">PluginExample1 </span><span class="java8">{<br />
  <br />
    </span><span class="java4">private </span><span class="java9">int </span><span class="java10">inputFld1;<br />
  <br />
    </span><span class="java4">private </span><span class="java9">int </span><span class="java10">intputFld2 = </span><span class="java7">50000</span><span class="java10">;<br />
  <br />
    </span><span class="java4">private </span><span class="java9">int </span><span class="java10">responseFld1;<br />
  <br />
    </span><span class="java4">private </span><span class="java9">int </span><span class="java10">responseFld2;<br />
    <br />
    </span><span class="java16">@Required<br />
    @RequestParameter<br />
    </span><span class="java4">public </span><span class="java9">void </span><span class="java10">setInputFld1</span><span class="java8">(</span><span class="java4">final </span><span class="java10">String inputfield1</span><span class="java8">) {<br />
      </span><span class="java4">this</span><span class="java10">.inputfield1 = inputfield1;<br />
    </span><span class="java8">}<br />
  <br />
    </span><span class="java16">@AllowedValues</span><span class="java8">( {<br />
      </span><span class="java5">&#34;1000000&#34;</span><span class="java10">, </span><span class="java5">&#34;500000&#34;</span><span class="java10">, </span><span class="java5">&#34;250000&#34;</span><span class="java10">, </span><span class="java5">&#34;125000&#34;</span><span class="java10">, </span><span class="java5">&#34;50000&#34;</span><span class="java10">, </span><span class="java5">&#34;25000&#34;</span><span class="java10">, </span><span class="java5">&#34;20000&#34;</span><span class="java10">,<br />
      </span><span class="java5">&#34;10000&#34;</span><span class="java10">, </span><span class="java5">&#34;5000&#34;</span><span class="java10">, </span><span class="java5">&#34;2500&#34;</span><span class="java10">, </span><span class="java5">&#34;2000&#34;</span><span class="java10">, </span><span class="java5">&#34;1250&#34;</span><span class="java10">, </span><span class="java5">&#34;1000&#34;</span><span class="java10">, </span><span class="java5">&#34;500&#34;<br />
    </span><span class="java8">})<br />
    </span><span class="java16">@JobParameter<br />
    @RequestParameter<br />
    </span><span class="java4">public </span><span class="java9">void </span><span class="java10">setInputField2</span><span class="java8">(</span><span class="java4">final </span><span class="java9">int </span><span class="java10">intputFld2</span><span class="java8">) {<br />
      </span><span class="java4">this</span><span class="java10">.intputFld2 = intputFld2;<br />
    </span><span class="java8">}<br />
  <br />
    </span><span class="java16">@ResponseField<br />
    </span><span class="java4">public </span><span class="java10">String getResponseFld1</span><span class="java8">() {<br />
      </span><span class="java4">return </span><span class="java10">responseFld1;<br />
    </span><span class="java8">}<br />
  <br />
    </span><span class="java16">@ResponseField<br />
    </span><span class="java4">public </span><span class="java9">int </span><span class="java10">getResponseFld2</span><span class="java8">() {<br />
      </span><span class="java4">return </span><span class="java10">responseFld21;<br />
    </span><span class="java8">}<br />
  <br />
    </span><span class="java4">public </span><span class="java9">void </span><span class="java10">execute</span><span class="java8">() {<br />
      </span><span class="java10">responseFld1 = doSomething</span><span class="java8">(</span><span class="java10">inputFld1, inputFld2</span><span class="java8">)</span><span class="java10">;<br />
      </span><span class="java4">if </span><span class="java8">(</span><span class="java10">responseFld1 &lt;= </span><span class="java7">0</span><span class="java8">) {<br />
        </span><span class="java4">throw new </span><span class="java10">IllegalArgumentException</span><span class="java8">(</span><span class="java5">&#34;invalid inputFld1&#34;</span><span class="java8">)</span><span class="java10">;<br />
      </span><span class="java8">} </span><span class="java4">else </span><span class="java8">{<br />
        </span><span class="java10">responseFld2 = doSomethingElse</span><span class="java8">(</span><span class="java10">inputFld1, inputFld2</span><span class="java8">)</span><span class="java10">;<br />
        </span><span class="java4">if </span><span class="java8">(</span><span class="java10">responseFld2 &lt;= </span><span class="java7">0</span><span class="java8">) {<br />
          </span><span class="java4">throw new </span><span class="java10">IllegalArgumentException</span><span class="java8">(</span><span class="java5">&#34;invalid inputFld2&#34;</span><span class="java8">)</span><span class="java10">;<br />
        </span><span class="java8">}<br />
      }<br />
    }<br />
    <br />
    </span><span class="java4">private </span><span class="java9">int </span><span class="java10">doSomething</span><span class="java8">(</span><span class="java4">final </span><span class="java9">int </span><span class="java10">input1, </span><span class="java4">final </span><span class="java9">int </span><span class="java10">input2</span><span class="java8">) {<br />
      </span><span class="java4">return </span><span class="java10">input1 * input2;<br />
    </span><span class="java8">}<br />
  <br />
    </span><span class="java4">private </span><span class="java9">int </span><span class="java10">doSomethingElse</span><span class="java8">(</span><span class="java4">final </span><span class="java9">int </span><span class="java10">input1, </span><span class="java4">final </span><span class="java9">int </span><span class="java10">input2</span><span class="java8">) {<br />
      </span><span class="java4">return </span><span class="java10">input1 - input2;<br />
    </span><span class="java8">}<br />
  }</span></pre></div>
  
  <p>
    <a href="../sampleCode//PluginExample1.java" class="button" title="download code for PluginExample1.java">download code</a>
  </p>
</div>
