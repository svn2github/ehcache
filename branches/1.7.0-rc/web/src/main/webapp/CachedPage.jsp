<%@ page import="java.util.Random" %>
<%@ page contentType="text/html; charset=utf-8" %>
<!--<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">-->

<%
    response.setDateHeader("Last-Modified", System.currentTimeMillis());
%>
<html>
<head>
    <title>Cached Page</title>
    <link href="/stylesheets/styles.css" rel="stylesheet" type="text/css">
</head>
<%@ include file="/include/Common.jsp" %>
Unicode Text: <%="М м Б Ц ц" %>
Here are some symbols: ↑ (&#8593;)
<p/>
Here are some Cyrillic characters: М м Б Ц ц (&#1052; &#1084; &#1041; &#1062; &#1094;)
<p/>
Here are some Cyrillic characters: <%="М м Б Ц ц" %>
<p/>
Here is a chunk of randomly generated text which takes 5 seconds to produce:
<p/>
<%
    Thread.sleep(5000);

    Random random = new Random();
    for (int i = 0; i < 10000; i++) {

    out.print((char)random.nextInt(255));
}
%>
<jsp:include page="/include/Footer.jsp"/>