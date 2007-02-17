<%@ page contentType="text/html; charset=utf-8" pageEncoding="utf-8" %>
<%-- If content type is not set Tomcat 5.5 at least sets it to text/html when used in a jsp:include --%>
<%@ page import="java.text.SimpleDateFormat,
                 java.util.Date"%>
<%
    final SimpleDateFormat format = new SimpleDateFormat("ddMMMyyyy HH:mm:ss.S");
    String date = format.format(new Date());
%>
<!-- Generated at <%= date %> -->
</body>

</html>