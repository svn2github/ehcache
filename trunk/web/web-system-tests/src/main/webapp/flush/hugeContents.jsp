<%@ page import="java.text.SimpleDateFormat,
                 java.util.Date"%>
<html>
<body>

<%
    final SimpleDateFormat format = new SimpleDateFormat("ddMMMyyyy HH:mm:ss.S");
    String date = format.format(new Date());
%>
<!-- Generated at <%= date %> -->

Start of flush buffer test.
(Buffer size: <%= response.getBufferSize() %>)

<p>
<%
 final int size = 64 * 1024;

 for (int i=0; i<size ;i++) {
     if (i > 0 && (i % 80) == 0)
         out.println("<br/>");
     out.print("" + i);
 }
%>
</p>

End of flush buffer test.
(Buffer size: <%= response.getBufferSize() %>)

</body>
</html>
