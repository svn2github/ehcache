<%
    response.setDateHeader("Last-Modified", System.currentTimeMillis());
    response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
%>
<%@ page session="false" %>
<html>
This must be removed from the response under RFC2616.
</html>