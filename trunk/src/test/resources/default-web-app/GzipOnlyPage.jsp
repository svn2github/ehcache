<%@ page import="java.util.Date"%>
<%
    //
    //wget -d --server-response --timestamping --header='Accept-Encoding: gzip' http://localhost:8080/GzipOnlyPage.jsp
    //wget -d --server-response --header='If-modified-Since: Fri, 12 May 2006 23:54:18 GMT' --header='Accept-Encoding: gzip' http://localhost:8080/GzipOnlyPage.jsp


    response.setDateHeader("Last-Modified", new Date(1234567).getTime());
    response.setHeader("Cache-Control", "public");
//    if (request.getHeader("If-modified-Since") != null) {
//        response.setStatus(304);
//    }


%>
<%@ page session="false" %>
<html>
<body>
This page is only included in the gzip filter, not in any caching filters and is thus a pure gzip page.
</body>
</html>