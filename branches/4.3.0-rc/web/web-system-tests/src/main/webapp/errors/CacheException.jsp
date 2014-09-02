<%@ page import="net.sf.ehcache.CacheException"%>
<%
    throw new CacheException("This is a cache exception message");
%>