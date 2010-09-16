<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.google.appengine.api.users.User" %>
<%@ page import="com.google.appengine.api.users.UserService" %>
<%@ page import="com.google.appengine.api.users.UserServiceFactory" %>
<%@ page import="net.sf.ehcache.gregrlucksamplejpaapp.EMF" %>
<%@ page import="net.sf.ehcache.gregrlucksamplejpaapp.guestbook.Greeting" %>
<%@ page import="java.util.List" %>

<html>
<body>

<%
    UserService userService = UserServiceFactory.getUserService();
    User user = userService.getCurrentUser();
    if (user != null) {
%>
<p>Hello, <%= user.getNickname() %>! (You can
    <a href="<%= userService.createLogoutURL(request.getRequestURI()) %>">sign out</a>.)</p>
<%
} else {
%>
<p>Hello!
    <a href="<%= userService.createLoginURL(request.getRequestURI()) %>">Sign in</a>
    to include your name with greetings you post.</p>
<%
    }
%>

<%
    EntityManager em = EMF.get().createEntityManager();
    javax.persistence.Query q = em.createQuery("SELECT g FROM Greeting g");
    q.setMaxResults(100);
    List<Greeting> greetings = (List<Greeting>) q.getResultList();
    if (greetings.isEmpty()) {
%>
<p>The guestbook has no messages.</p>
<%
} else {
    for (Greeting g : greetings) {
        if (g.getAuthor() == null) {
%>
<p>An anonymous person wrote:</p>
<%
} else {
%>
<p><b><%= g.getAuthor().getNickname() %>
</b> wrote:</p>
<%
    }
%>
<blockquote><%= g.getContent() %>
</blockquote>
<%
        }
    }
    em.close();
%>

<form action="/sign" method="post">
    <div><textarea name="content" rows="3" cols="60"></textarea></div>
    <div><input type="submit" value="Post Greeting"/></div>
</form>

</body>
</html>