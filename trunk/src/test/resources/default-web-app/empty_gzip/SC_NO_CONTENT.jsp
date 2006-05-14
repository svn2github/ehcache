<%
    response.setDateHeader("Last-Modified", System.currentTimeMillis());
    response.setStatus(HttpServletResponse.SC_NO_CONTENT);
%>
<html>
This must be removed from the response under RFC2616.
</html>