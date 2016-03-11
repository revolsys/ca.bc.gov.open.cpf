<%@ page
  contentType="application/xhtml+xml; charset=UTF-8"
  pageEncoding="UTF-8"
  session="true"
%><%@
  taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"
%><%@
  taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"
%><?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.1//EN"
"http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">
  <head>
    <title><c:out value="${title}" /></title>
<c:forEach var="cssLink" items="${cssLinks}"
>    <link href="<c:url value="${cssLink}" />" rel="stylesheet" type="text/css" /></c:forEach>
<c:forEach var="javascriptLink" items="${javascriptLinks}"
>    <script src="<c:url value="${javascriptLink}" />" type="text/javascript">
    </script></c:forEach>
  </head>

  <body>
    <div class="header"><c:choose>
      <c:when test="${empty(pageHeader)}"><c:out value="${title}" /></c:when>
      <c:when test="${fn:startsWith(pageHeader, 'forward:')}"><c:import url="${fn:substringAfter(pageHeader, 'forward:')}"/></c:when>
      <c:otherwise><c:out value="${pageHeader}" /></c:otherwise>
    </c:choose></div>
    <div class="body">
      <div class="loginForm">
        <a href="http://openid.net"><img src="<c:url value="${baseUrl}/images/openid-logo.png" />" alt="OpenID" /></a>
        <div class="title"><c:out value="${pageContext.request.serverName}" /> requires you to logon</div>
        <div class="fields">
          <form id="openIdLoginForm" action="<c:url value="${baseUrl}/j_spring_openid_security_check" />" method="post">
             <dl>
              <dt>Your OpenID URL:</dt>
              <dd><input id="openid_identifier" name="openid_identifier" size="70" /><br />
              <span style="font-size: 0.8em; color: #999999">e.g.
              openid.aol.com/<b>screenname</b>,
              <b>blogname</b>.blogspot.com,
              www.flickr.com/photos/<b>username</b>,
              <b>username</b>.livejournal.com,
              <b>username</b>.wordpress.com,
              <b>username</b>@yahoo.com
              </span></dd>
            </dl>
             <dl>
              <dt>OpenID Provider:</dt>
              <dd>
                <a href="#" onclick="loginUsing('www.flickr.com')" class="signIn"><img src="<c:url value="${baseUrl}/images/flickr.png" />" alt="Flickr" /> Sign in With Flickr</a><br />
                <a href="#" onclick="loginUsing('https://www.google.com/accounts/o8/id')" class="signIn"><img src="<c:url value="${baseUrl}/images/google.png" />" alt="Google" /> Sign in With Google</a><br />
                <a href="#" onclick="loginUsing('wordpress.com')" class="signIn"><img src="<c:url value="${baseUrl}/images/wordpress.png" />" alt="Wordpress" /> Sign in With Wordpress</a><br />
                <a href="#" onclick="loginUsing('yahoo.com')" class="signIn"><img src="<c:url value="${baseUrl}/images/yahoo.png" />" alt="Yahoo" /> Sign in With Yahoo</a><br />
                <a href="http://openid.net/get/" target="_blank" class="signIn"><img src="<c:url value="${baseUrl}/images/openid-inputicon.gif" />" alt="OpenId" />List of other providers</a>
                </dd>
            </dl>
            <div style="text-align: right">
              <input class="button" type="submit" value="Next" />
            </div>
          </form>
        </div>
        <c:if test="${!empty sessionScope['SPRING_SECURITY_LAST_EXCEPTION']}">
         <div class="alert alert-danger" role="alert"><c:out value="${sessionScope['SPRING_SECURITY_LAST_EXCEPTION'].message}" /></div>
        </c:if>
        <div class="disclaimer">Access to or unauthorized use of data on this
        computer system by any person other than the authorized employee(s) or
        owner(s) of an account is strictly prohibited and may result in legal
        action  against such person.</div>
      </div>
      
    </div>
    <c:if test="${!empty(pageFooter)}">
    <div class="footer"><c:choose>
      <c:when test="${fn:startsWith(pageFooter, 'forward:')}"><c:import url="${fn:substringAfter(pageFooter, 'forward:')}"/></c:when>
      <c:otherwise><c:out value="${pageFooter}" /></c:otherwise>
    </c:choose></div>
    </c:if>
  </body>
</html>