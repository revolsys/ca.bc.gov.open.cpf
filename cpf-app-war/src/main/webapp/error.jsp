<%@ page
  contentType="text/html; charset=UTF-8"
  pageEncoding="UTF-8"
  session="false"
%><%@
  taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %><?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html>
<html xml:lang="en">
<head>
<title><c:out value="${requestScope['javax.servlet.error.status_code']}" /> Error</title>
<link href="<c:url value="/css/rs.css" />" rel="stylesheet" type="text/css" />
<link href="<c:url value="/css/cpf.css" />" rel="stylesheet" type="text/css" />
<link href="<c:url value="/css/bcgov.css" />" rel="stylesheet" type="text/css" />
<style type="text/css">
div.body {
  margin: 10px auto 10px auto;
  width: 80%;
}

div.content {
  clear: both;
  margin: 20px 0px 20px 0px;
}

div.content div.title {
  background-color: #6699cc;
  border-bottom: 1px solid #6699cc;
  color: white;
  font: 1.3em "Times New Roman", Times, serif;
  height: 26px;
  padding: 5px 0px 0px 20px;
  margin: 0px auto 5px 0px;
}

div.content div.message {
  border: 1px solid #999999;
  padding: 15px;
}

.subtitle {
  font: 1.1em "Times New Roman", Times, serif;
  color: 0066cc;
}

li {
  margin-bottom: 10px;
}

.errorMessage {
  overflow: auto;
  height: 50%;
  width: 90%;
}
</style>
</head>

<body>
  <c:choose>
    <c:when test="${param.plain == 'true'}">
      <div class="title">
        <c:out value="${requestScope['javax.servlet.error.status_code']}" />
        Error
      </div>
      <div class="message">
        <c:if test="${requestScope['javax.servlet.error.message'] != null}">
          <div class="errorMessage">
            <c:out value="${requestScope['javax.servlet.error.message']}" escapeXml="false" />
          </div>
        </c:if>
        <ul>
          <li>To return to the previous page <a href="javascript:history.go(-1)">click here</a></li>
          <li>To return to the home page <a href="<c:url value="/" />">click here</a></li>
        </ul>
      </div>
    </c:when>
    <c:otherwise>
      <div class="body">
        <div class="header">
          <div class="headerContent">
            <div class="title">Concurrent Processing Framework</div>
          </div>
        </div>
        <div class="content">
          <div class="title">
            <c:out value="${requestScope['javax.servlet.error.status_code']}" />
            Error
          </div>
          <div class="message">
            <c:if test="${requestScope['javax.servlet.error.message'] != null}">
              <div class="errorMessage">
                <c:out value="${requestScope['javax.servlet.error.message']}" escapeXml="false" />
              </div>
            </c:if>
            <ul>
              <li>To return to the previous page <a href="javascript:history.go(-1)">click
                  here</a></li>
              <li>To return to the home page <a href="<c:url value="/" />">click here</a></li>
            </ul>
          </div>
        </div>
      </div>
    </c:otherwise>
  </c:choose>
</body>
</html>
