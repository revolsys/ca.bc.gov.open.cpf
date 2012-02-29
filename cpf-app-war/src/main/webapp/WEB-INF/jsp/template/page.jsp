<%@ page
  contentType="text/html; charset=UTF-8"
  pageEncoding="UTF-8"
  session="false"
%><%@
  taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"
%><?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en"<c:if test="${!empty(param.htmlCss)}"> class="<c:out value="${param.htmlCss}" />"</c:if>>
  <head>
    <title><c:out value="${title}" /></title>
    
<c:forEach var="cssUrl" items="${cssUrls}">
    <link href="<c:url value="${cssUrl}" />" rel="stylesheet" type="text/css" />
</c:forEach>
    <link href="<c:url value="/css/bcgov.css" />" rel="stylesheet" type="text/css" />
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
<c:choose>
  <c:when test="${param.plain == 'true'}">
    <c:if test="${!empty(body)}">
<c:import url="${body}" />
    </c:if>
  </c:when>
  <c:otherwise>
    <div class="body">
      <div class="header">
        <div class="headerContent">
          <div class="title">Cloud Processing Framework</div>
        </div>
      </div>
      <div class="columns">
        <div>
          <div class="sideNav">
            <c:import url="/view/menu/sideMenu" />
          </div>
          <div class="bodyContainer">
            <div class="breadcrumbs">  <c:if test="${!empty(breadcrumbUrl)}">
          <c:import url="${breadcrumbUrl}" />
    </c:if>
  </div>
            <div class="bodyContent">
           <c:if test="${!empty(pageHeading)}">
            <h1><c:out value="${pageHeading}" /></h1>
            </c:if>
  <c:if test="${!empty(body)}">
            <c:import url="${body}" />
  </c:if>
            </div>
          </div>
        </div>
      </div>
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
</c:otherwise>
</c:choose>
  </body>
</html>