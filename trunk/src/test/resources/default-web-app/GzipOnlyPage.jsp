<%@ page contentType="text/html; charset=utf-8" pageEncoding="utf-8" %>
<%@ page import="java.util.Date"%>
<%
    //
    //wget -d --server-response --timestamping --header='Accept-Encoding: gzip' http://localhost:9080/GzipOnlyPage.jsp
    //wget -d --server-response --header='If-modified-Since: Fri, 12 May 2006 23:54:18 GMT' --header='Accept-Encoding: gzip' http://localhost:9080/GzipOnlyPage.jsp


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
<p/>
Here are some symbols:  ↑ (&#8593;)
<p/>
Here are some Cyrillic characters:  М м Б Ц ц (&#1052; &#1084; &#1041; &#1062; &#1094;)
<p/>
Here are some Cyrillic characters: <%="М м Б Ц ц" %>
<p/>
Here is a symbol: <%="↑" %>
</body>
</html>