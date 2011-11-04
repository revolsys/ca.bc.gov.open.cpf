<%@ page
  contentType="text/html; charset=UTF-8"
  pageEncoding="UTF-8"
  session="false"
%><%@
  taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"
%><?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.1//EN"
"http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">
  <head>
    <title><c:out value="${title}" /></title>
    
    <link href="<c:url value="/css/bcgov.css" />" rel="stylesheet" type="text/css" />
<c:forEach var="cssUrl" items="${cssUrls}">
    <link href="<c:url value="${cssUrl}" />" rel="stylesheet" type="text/css" />
</c:forEach>
<c:forEach var="javascriptUrl" items="${javascriptUrls}">
    <script type="text/javascript" src="<c:url value="${javascriptUrl}" />">
    </script>
</c:forEach>
<c:forEach var="alternateLink" items="${alternateLinks}">    <link rel="alternate"
      type="<c:out value="${alternateLink.type}" />"
      title="<c:out value="${alternateLink.title}" />"
      href="<c:out value="${alternateLink.href}" />" />
</c:forEach>
  </head>
  <body>
<c:if test="${param.plain != 'true'}">
    <div class="body">
      <div class="header">
        <div class="headerContent">
        </div>
      </div>
      <div class="sideNav">
        <div class="sideMenu">
          <div class="title"><a href="http://www.gov.bc.ca/" title="B.C. Home">B.C. Home</a></div>
          <ul>
            <li><a href="<c:url value="/" />" title="Home">Home</a></li>
<c:forEach var="item" items="${sideMenu}"
>            <li><a href="<c:url value="${item.link}"/>" title="<c:out value="${item.value}" />"><c:out value="${item.value}" /></a></li>
</c:forEach>
          </ul>
        </div>
      </div>
      
      <div class="breadcrumbs">  <c:if test="${!empty(breadcrumbUrl)}">
        <c:import url="${breadcrumbUrl}" />
  </c:if>
</div>
</c:if>
      <div class="bodyContent">
<c:if test="${!empty(body)}">
          <c:import url="${body}" />
</c:if>
      </div>
<c:if test="${param.plain != 'true'}">
     
      <div class="footerMenu">
        <div class="title"><c:out value="${applicationName}" /> (v <c:out value="${applicationVersion}" />)</div>
        <ul>
          <li><a href="http://www.gov.bc.ca/com/copy" title="COPYRIGHT">COPYRIGHT</a></li>
          <li><a href="http://www.gov.bc.ca/com/disc" title="DISCLAIMER">DISCLAIMER</a></li>
          <li><a href="http://www.gov.bc.ca/com/priv" title="PRIVACY">PRIVACY</a></li>
          <li><a href="http://www.gov.bc.ca/com/accessibility" title="ACCESSIBILITY" >ACCESSIBILITY</a></li>
        </ul>
      </div>
    </div>
</c:if>
  </body>
</html>