<%@ page import="java.text.SimpleDateFormat,
                 java.util.Date"%>
<%@ page contentType="text/html; charset=utf-8" %>
<%
    final SimpleDateFormat format = new SimpleDateFormat("ddMMMyyyy HH:mm:ss.S");
    String date = format.format(new Date());
%>
<!-- Generated at <%= date %> -->
