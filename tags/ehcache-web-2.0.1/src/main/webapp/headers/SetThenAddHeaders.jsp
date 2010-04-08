<%@ page import="java.text.SimpleDateFormat,
                 java.util.Date"%>
<%
    response.setHeader("x-sample-header", "sample value 1");
    response.addHeader("x-sample-header", "sample value 2");
    response.addHeader("x-sample-header", "sample value 3");
    response.addHeader("x-sample-header", "sample value 4");
%>
<html>
<body>

<%
    final SimpleDateFormat format = new SimpleDateFormat("ddMMMyyyy HH:mm:ss.S");
    String date = format.format(new Date());
%>
<!-- Generated at <%= date %> -->

Headers test.

</body>
</html>
