<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page import="java.util.Date"%>
<%
    //wget -d --server-response --timestamping --header='Accept-Encoding: gzip' http://localhost:8080/GzipOnlyPage.jsp
    //wget -d --server-response --header='If-modified-Since: Fri, 12 May 2006 23:54:18 GMT' --header='Accept-Encoding: gzip' http://localhost:8080/GzipOnlyPage.jsp

    response.setDateHeader("Last-Modified", new Date(1234567).getTime());
    response.setHeader("Cache-Control", "public");
%>
<%@ page session="false" %>
<html>
<body>
This page is not filtered at all.
<p/>
Here are some Cyrillic characters: <%="М м Б Ц ц" %>
<p/>
Here is a symbol: <%="↑" %>

</body>
</html>
