<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false"%><%@
  taglib
  uri="http://java.sun.com/jsp/jstl/core" prefix="c"%><?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html>
<html xml:lang="en">
<head>
<title><c:out value="${title}" /></title>
<meta http-equiv="X-UA-Compatible" content="IE=8,IE=9,IE=10" />
<c:forEach var="cssUrl" items="${cssUrls}">
  <link href="<c:url value="${cssUrl}" />" rel="stylesheet" type="text/css" />
</c:forEach>
<link href="<c:url value="/css/bcgov.css" />" rel="stylesheet" type="text/css" />
<c:forEach var="javascriptUrl" items="${javascriptUrls}">
  <script type="text/javascript" src="<c:url value="${javascriptUrl}" />">
      
    </script>
</c:forEach>
<c:forEach var="alternateLink" items="${alternateLinks}">
  <link rel="alternate" type="<c:out value="${alternateLink.type}" />"
    title="<c:out value="${alternateLink.title}" />" href="<c:out value="${alternateLink.href}" />" />
</c:forEach>
</head>
<body>
  <c:choose>
    <c:when test="${param.plain == 'true'}">
      <div class="plain">
        <c:if test="${!empty(body)}">
          <c:import url="${body}" charEncoding="UTF-8" />
        </c:if>
      </div>
    </c:when>
    <c:otherwise>
      <div class="body">
        <div class="header">
          <div class="headerContent">
            <div class="title">Cloud Processing Framework</div>
          </div>
        </div>
        <div class="columns_2_2">
          <div class="columns_2_1">
            <div>
              <div class="column_2_1">
                <div class="sideNav">
                  <c:import url="/view/menu/sideMenu" charEncoding="UTF-8" />
                </div>
              </div>
              <div class="column_2_2">
                <div class="bodyContainer">
                  <div class="breadcrumbs">
                    <c:if test="${!empty(breadcrumbUrl)}">
                      <c:import url="${breadcrumbUrl}" charEncoding="UTF-8" />
                    </c:if>
                  </div>
                  <div class="bodyContent">
                    <c:if test="${!empty(pageHeading)}">
                      <h1>
                        <c:out value="${pageHeading}" />
                      </h1>
                    </c:if>
                    <c:if test="${!empty(body)}">
                      <c:import url="${body}" charEncoding="UTF-8" />
                    </c:if>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div class="footer">
          <div class="title">
            <c:out value="${applicationName}" />
            (v
            <c:out value="${applicationVersion}" />
            )
          </div>
          <c:import url="/view/menu/footerMenu" charEncoding="UTF-8" />
        </div>
      </div>
    </c:otherwise>
  </c:choose>
</body>
</html>