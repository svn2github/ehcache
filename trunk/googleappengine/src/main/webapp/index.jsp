<%@ page import="net.sf.ehcache.CacheManager" %>
<%@ page import="net.sf.ehcache.Cache" %>
<%@ page import="java.util.Date" %>
<%@ page import="net.sf.ehcache.Element" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
    <title>Welcome</title>
</head>
<body>
Welcome to Google App Engine for Java Cache Test.

<%
    Cache cache = CacheManager.getInstance().getCache("sampleCache1");
    cache.put(new Element(new Date(), new byte[10485760]));
    out.print("The cache now has  " + cache.getSize() * 10 + " MB in memory." );


    out.print("Request this page again to increase by another 10MB." );




%>


<img src="images/cleancode.jpg" alt="Clean Code"/>

</body>
</html>