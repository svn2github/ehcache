<%@ page contentType="text/html; charset=utf-8" %>
<%@ page import="net.sf.ehcache.constructs.web.filter.Filter"%>
<%
    request.setAttribute(Filter.NO_FILTER, "true"); // prevent further filter use for jsp:includes
%>
<jsp:include page="../CachedPage.jsp"/>
<% return; %>