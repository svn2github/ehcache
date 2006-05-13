<%
    response.setDateHeader("Last-Modified", System.currentTimeMillis());
    response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
%>