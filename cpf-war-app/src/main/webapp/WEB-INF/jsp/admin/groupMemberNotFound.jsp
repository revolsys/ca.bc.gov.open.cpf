<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"
  session="false"
%><%@
  taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%><div
  style="padding: 0px 5px 0px 5px"
>
  <h1>User Not Found</h1>
  <p>
    The user
    <c:out value="${consumerKey}" />
    could not be found. For external user accounts the user must have logged
    into the CPF Web Services for a proxy account to automatically be created.
  </p>

  <p>
    <a href="javascript:history.go(-1)">click here</a> to return to the previous
    page.
  </p>
</div>