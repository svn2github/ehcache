<%@ page import="java.text.SimpleDateFormat,
                 java.util.Date"%>
<%
    final SimpleDateFormat format = new SimpleDateFormat("ddMMMyyyy HH:mm:ss.S");
    String date = format.format(new Date());
%>
<!-- Generated at <%= date %> -->

Unicode Text: <%="? ? ? ? ?" %>
Here are some symbols: ? (&#8593;)
<p/>
Here are some Cyrillic characters: ? ? ? ? ? (&#1052; &#1084; &#1041; &#1062; &#1094;)
<p/>
Here are some Cyrillic characters: <%="? ? ? ? ?" %>
<p/>
Here is a chunk of randomly generated text which takes 5 seconds to produce:
<p/>

