<%@ page import="java.text.SimpleDateFormat,
                 java.util.Date"%>
<%--<%@ page contentType="text/html; charset=utf-8" pageEncoding="utf-8" %>--%>
<%
    final SimpleDateFormat format = new SimpleDateFormat("ddMMMyyyy HH:mm:ss.S");
    String date = format.format(new Date());
%>
<b>Generated at <%= date %></b>
</p>
