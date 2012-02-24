<div>
  <h1>HTML Code to Submit a New Batch Job</h1>
  
  <pre class="precode">
  <span style="color:#7f0055; font-weight:bold">&lt;html</span> xmlns=<span style="color:#0000ff">&quot;http://www.w3.org/1999/xhtml&quot;</span><span style="color:#7f0055; font-weight:bold">&gt;</span>
  <span style="color:#7f0055; font-weight:bold">&lt;head&gt;</span>
    <span style="color:#7f0055; font-weight:bold">&lt;title&gt;</span>Submit Job<span style="color:#7f0055; font-weight:bold">&lt;/title&gt;</span>
  <span style="color:#7f0055; font-weight:bold">&lt;/head&gt;</span>
  <span style="color:#7f0055; font-weight:bold">&lt;body&gt;</span>
    <span style="color:#7f0055; font-weight:bold">&lt;form</span> method=<span style="color:#0000ff">&quot;post&quot;</span> enctype=<span style="color:#0000ff">&quot;multipart/form-data&quot;</span> action=<span style="color:#0000ff">&quot;http://WebServicesUrl/ws/ws/apps/mapTileByLocation/1.0.0/jobs&quot;</span><span style="color:#7f0055; font-weight:bold">&gt;</span>
      <span style="color:#7f0055; font-weight:bold">&lt;dl&gt;</span>
        <span style="color:#7f0055; font-weight:bold">&lt;dt&gt;</span>
          <span style="color:#7f0055; font-weight:bold">&lt;label</span> for=<span style="color:#0000ff">&quot;authUserId&quot;</span>&gt;User ID<span style="color:#7f0055; font-weight:bold">&lt;/label&gt;</span>
        <span style="color:#7f0055; font-weight:bold">&lt;/dt&gt;</span>
        <span style="color:#7f0055; font-weight:bold">&lt;dd&gt;</span>
          <span style="color:#7f0055; font-weight:bold">&lt;input</span> name=<span style="color:#0000ff">&quot;authUserId&quot;</span> type=<span style="color:#0000ff">&quot;text&quot;</span> size=<span style="color:#0000ff">&quot;50&quot;</span> maxlength=<span style="color:#0000ff">&quot;255&quot;</span> <span style="color:#7f0055; font-weight:bold">/&gt;</span>
        <span style="color:#7f0055; font-weight:bold">&lt;/dd&gt;</span>
        <span style="color:#7f0055; font-weight:bold">&lt;dt&gt;</span>
          <span style="color:#7f0055; font-weight:bold">&lt;label</span> for=<span style="color:#0000ff">&quot;numRequests&quot;</span>&gt;Num Requests<span style="color:#7f0055; font-weight:bold">&lt;/label&gt;</span>
        <span style="color:#7f0055; font-weight:bold">&lt;/dt&gt;</span>
        <span style="color:#7f0055; font-weight:bold">&lt;dd&gt;</span>
          <span style="color:#7f0055; font-weight:bold">&lt;input</span> name=<span style="color:#0000ff">&quot;numRequests&quot;</span> type=<span style="color:#0000ff">&quot;text&quot;</span> size=<span style="color:#0000ff">&quot;50&quot;</span> maxlength=<span style="color:#0000ff">&quot;255&quot;</span> <span style="color:#7f0055; font-weight:bold">/&gt;</span>
        <span style="color:#7f0055; font-weight:bold">&lt;/dd&gt;</span>
        <span style="color:#7f0055; font-weight:bold">&lt;dt&gt;</span>
          <span style="color:#7f0055; font-weight:bold">&lt;label</span> for=<span style="color:#0000ff">&quot;inputDataContentType&quot;</span>&gt;Input Data Content Type<span style="color:#7f0055; font-weight:bold">&lt;/label&gt;</span>
        <span style="color:#7f0055; font-weight:bold">&lt;/dt&gt;</span>
        <span style="color:#7f0055; font-weight:bold">&lt;dd&gt;</span>
          <span style="color:#7f0055; font-weight:bold">&lt;select</span> name=<span style="color:#0000ff">&quot;inputDataContentType&quot;</span><span style="color:#7f0055; font-weight:bold">&gt;</span>
            <span style="color:#7f0055; font-weight:bold">&lt;option&gt;</span>text/csv<span style="color:#7f0055; font-weight:bold">&lt;/option&gt;</span>
            <span style="color:#7f0055; font-weight:bold">&lt;option&gt;</span>application/json<span style="color:#7f0055; font-weight:bold">&lt;/option&gt;</span>
          <span style="color:#7f0055; font-weight:bold">&lt;/select&gt;</span>
        <span style="color:#7f0055; font-weight:bold">&lt;/dd&gt;</span>
        <span style="color:#7f0055; font-weight:bold">&lt;dt&gt;</span>
          <span style="color:#7f0055; font-weight:bold">&lt;label</span> for=<span style="color:#0000ff">&quot;inputDataUrl&quot;</span>&gt;Input Data Url<span style="color:#7f0055; font-weight:bold">&lt;/label&gt;</span>
        <span style="color:#7f0055; font-weight:bold">&lt;/dt&gt;</span>
        <span style="color:#7f0055; font-weight:bold">&lt;dd&gt;</span>
          <span style="color:#7f0055; font-weight:bold">&lt;input</span> name=<span style="color:#0000ff">&quot;inputDataUrl&quot;</span> type=<span style="color:#0000ff">&quot;text&quot;</span> size=<span style="color:#0000ff">&quot;50&quot;</span> maxlength=<span style="color:#0000ff">&quot;255&quot;</span> <span style="color:#7f0055; font-weight:bold">/&gt;</span>
        <span style="color:#7f0055; font-weight:bold">&lt;/dd&gt;</span>
        <span style="color:#7f0055; font-weight:bold">&lt;dt&gt;</span>
          <span style="color:#7f0055; font-weight:bold">&lt;label</span> for=<span style="color:#0000ff">&quot;inputData&quot;</span>&gt;Input Data<span style="color:#7f0055; font-weight:bold">&lt;/label&gt;</span>
        <span style="color:#7f0055; font-weight:bold">&lt;/dt&gt;</span>
        <span style="color:#7f0055; font-weight:bold">&lt;dd&gt;</span>
          <span style="color:#7f0055; font-weight:bold">&lt;input</span> name=<span style="color:#0000ff">&quot;inputData&quot;</span> type=<span style="color:#0000ff">&quot;file&quot;</span> <span style="color:#7f0055; font-weight:bold">/&gt;</span>
        <span style="color:#7f0055; font-weight:bold">&lt;/dd&gt;</span>
        <span style="color:#7f0055; font-weight:bold">&lt;dt&gt;</span>
          <span style="color:#7f0055; font-weight:bold">&lt;label</span> for=<span style="color:#0000ff">&quot;resultDataContentType&quot;</span>&gt;Result Data Content Type<span style="color:#7f0055; font-weight:bold">&lt;/label&gt;</span>
        <span style="color:#7f0055; font-weight:bold">&lt;/dt&gt;</span>
        <span style="color:#7f0055; font-weight:bold">&lt;dd&gt;</span>
          <span style="color:#7f0055; font-weight:bold">&lt;select</span> name=<span style="color:#0000ff">&quot;resultDataContentType&quot;</span><span style="color:#7f0055; font-weight:bold">&gt;</span>
            <span style="color:#7f0055; font-weight:bold">&lt;option&gt;</span>text/csv<span style="color:#7f0055; font-weight:bold">&lt;/option&gt;</span>
            <span style="color:#7f0055; font-weight:bold">&lt;option&gt;</span>application/json<span style="color:#7f0055; font-weight:bold">&lt;/option&gt;</span>
            <span style="color:#7f0055; font-weight:bold">&lt;option&gt;</span>application/xhtml+xml<span style="color:#7f0055; font-weight:bold">&lt;/option&gt;</span>
            <span style="color:#7f0055; font-weight:bold">&lt;option&gt;</span>text/xml<span style="color:#7f0055; font-weight:bold">&lt;/option&gt;</span>
            <span style="color:#7f0055; font-weight:bold">&lt;option&gt;</span>application/vnd.google-earth.kml+xml<span style="color:#7f0055; font-weight:bold">&lt;/option&gt;</span>
          <span style="color:#7f0055; font-weight:bold">&lt;/select&gt;</span>
        <span style="color:#7f0055; font-weight:bold">&lt;/dd&gt;</span>
        <span style="color:#7f0055; font-weight:bold">&lt;dt&gt;</span>
          <span style="color:#7f0055; font-weight:bold">&lt;label</span> for=<span style="color:#0000ff">&quot;notificationUrl Url&quot;</span>&gt;Notification Url<span style="color:#7f0055; font-weight:bold">&lt;/label&gt;</span>
        <span style="color:#7f0055; font-weight:bold">&lt;/dt&gt;</span>
        <span style="color:#7f0055; font-weight:bold">&lt;dd&gt;</span>
          <span style="color:#7f0055; font-weight:bold">&lt;input</span> name=<span style="color:#0000ff">&quot;notificationUrl&quot;</span> type=<span style="color:#0000ff">&quot;text&quot;</span> size=<span style="color:#0000ff">&quot;50&quot;</span> maxlength=<span style="color:#0000ff">&quot;255&quot;</span> <span style="color:#7f0055; font-weight:bold">/&gt;</span>
        <span style="color:#7f0055; font-weight:bold">&lt;/dd&gt;</span>
        <span style="color:#7f0055; font-weight:bold">&lt;dt&gt;</span>
          <span style="color:#7f0055; font-weight:bold">&lt;label</span> for=<span style="color:#0000ff">&quot;mapGridName&quot;</span>&gt;Map Grid Name<span style="color:#7f0055; font-weight:bold">&lt;/label&gt;</span>
        <span style="color:#7f0055; font-weight:bold">&lt;/dt&gt;</span>
        <span style="color:#7f0055; font-weight:bold">&lt;dd&gt;</span>
          <span style="color:#7f0055; font-weight:bold">&lt;select</span> name=<span style="color:#0000ff">&quot;mapGridName&quot;</span><span style="color:#7f0055; font-weight:bold">&gt;</span>
            <span style="color:#7f0055; font-weight:bold">&lt;option</span> value=<span style="color:#0000ff">&quot;&quot;</span>&gt;-<span style="color:#7f0055; font-weight:bold">&lt;/option&gt;</span>
            <span style="color:#7f0055; font-weight:bold">&lt;option&gt;</span><span style="color:#000000">NTS 1:1 000 000</span><span style="color:#7f0055; font-weight:bold">&lt;/option&gt;</span>
            <span style="color:#7f0055; font-weight:bold">&lt;option&gt;</span><span style="color:#000000">NTS 1:500 000</span><span style="color:#7f0055; font-weight:bold">&lt;/option&gt;</span>
            <span style="color:#7f0055; font-weight:bold">&lt;option&gt;</span><span style="color:#000000">NTS 1:250 000</span><span style="color:#7f0055; font-weight:bold">&lt;/option&gt;</span>
            <span style="color:#7f0055; font-weight:bold">&lt;option&gt;</span><span style="color:#000000">NTS 1:125 000</span><span style="color:#7f0055; font-weight:bold">&lt;/option&gt;</span>
            <span style="color:#7f0055; font-weight:bold">&lt;option&gt;</span><span style="color:#000000">NTS 1:50 000</span><span style="color:#7f0055; font-weight:bold">&lt;/option&gt;</span>
            <span style="color:#7f0055; font-weight:bold">&lt;option&gt;</span><span style="color:#000000">NTS 1:25 000</span><span style="color:#7f0055; font-weight:bold">&lt;/option&gt;</span>
            <span style="color:#7f0055; font-weight:bold">&lt;option&gt;</span><span style="color:#000000">BCGS 1:20 000</span><span style="color:#7f0055; font-weight:bold">&lt;/option&gt;</span>
            <span style="color:#7f0055; font-weight:bold">&lt;option&gt;</span><span style="color:#000000">BCGS 1:10 000</span><span style="color:#7f0055; font-weight:bold">&lt;/option&gt;</span>
            <span style="color:#7f0055; font-weight:bold">&lt;option&gt;</span><span style="color:#000000">BCGS 1:5000</span><span style="color:#7f0055; font-weight:bold">&lt;/option&gt;</span>
            <span style="color:#7f0055; font-weight:bold">&lt;option&gt;</span><span style="color:#000000">BCGS 1:2500</span><span style="color:#7f0055; font-weight:bold">&lt;/option&gt;</span>
            <span style="color:#7f0055; font-weight:bold">&lt;option&gt;</span><span style="color:#000000">BCGS 1:2000</span><span style="color:#7f0055; font-weight:bold">&lt;/option&gt;</span>
            <span style="color:#7f0055; font-weight:bold">&lt;option&gt;</span><span style="color:#000000">BCGS 1:1250</span><span style="color:#7f0055; font-weight:bold">&lt;/option&gt;</span>
            <span style="color:#7f0055; font-weight:bold">&lt;option&gt;</span><span style="color:#000000">BCGS 1:1000</span><span style="color:#7f0055; font-weight:bold">&lt;/option&gt;</span>
            <span style="color:#7f0055; font-weight:bold">&lt;option&gt;</span><span style="color:#000000">BCGS 1:500</span><span style="color:#7f0055; font-weight:bold">&lt;/option&gt;</span>
            <span style="color:#7f0055; font-weight:bold">&lt;option&gt;</span><span style="color:#000000">MTO</span><span style="color:#7f0055; font-weight:bold">&lt;/option&gt;</span>
          <span style="color:#7f0055; font-weight:bold">&lt;/select&gt;</span>
        <span style="color:#7f0055; font-weight:bold">&lt;/dd&gt;</span>
        <span style="color:#7f0055; font-weight:bold">&lt;dt&gt;</span>
          <span style="color:#7f0055; font-weight:bold">&lt;label</span> for=<span style="color:#0000ff">&quot;numBoundaryPoints&quot;</span>&gt;Num Boundary Points<span style="color:#7f0055; font-weight:bold">&lt;/label&gt;</span>
        <span style="color:#7f0055; font-weight:bold">&lt;/dt&gt;</span>
        <span style="color:#7f0055; font-weight:bold">&lt;dd&gt;</span>
          <span style="color:#7f0055; font-weight:bold">&lt;input</span> name=<span style="color:#0000ff">&quot;numBoundaryPoints&quot;</span> type=<span style="color:#0000ff">&quot;text&quot;</span> size=<span style="color:#0000ff">&quot;50&quot;</span> maxlength=<span style="color:#0000ff">&quot;255&quot;</span> <span style="color:#7f0055; font-weight:bold">/&gt;</span>
        <span style="color:#7f0055; font-weight:bold">&lt;/dd&gt;</span>
      <span style="color:#7f0055; font-weight:bold">&lt;/dl&gt;</span>
      <span style="color:#7f0055; font-weight:bold">&lt;input</span> name=<span style="color:#0000ff">&quot;Submit&quot;</span> type=<span style="color:#0000ff">&quot;submit&quot;</span> value=<span style="color:#0000ff">&quot;submit&quot;</span> <span style="color:#7f0055; font-weight:bold">/&gt;</span>
    <span style="color:#7f0055; font-weight:bold">&lt;/form&gt;</span>
  <span style="color:#7f0055; font-weight:bold">&lt;/body&gt;</span>
  <span style="color:#7f0055; font-weight:bold">&lt;/html&gt;</span>
  </pre>

  <p>
    <a href="../sampleCode/WsFormExample.html.txt" class="button" target="_blank">download code</a>
  </p>
</div>
