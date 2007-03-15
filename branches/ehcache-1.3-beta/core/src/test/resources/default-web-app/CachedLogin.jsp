<%@ page contentType="text/html; charset=utf-8" %>
<!--<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">-->
<%@ include file="/include/Common.jsp" %>
<%
    response.setDateHeader("Last-Modified", System.currentTimeMillis());
%>
<html>
<head>
<title>Login Page</title>
<link href="/stylesheets/styles.css" rel="stylesheet" type="text/css">
</head>
<body onload="document.getElementById('input_username').focus()">

<form name="loginForm" method="post" action="j_security_check" target="_top">
	<table align="center" border="0" cellpadding="0" cellspacing="0" style="margin-top:1em;">
		<tr>
			<td><img src="/image/login_left_curve.gif"/></td>
			<td><img src="/image/user_login.gif"/></td>
			<td><img src="/image/login_right_curve.gif"/></td>
		</tr>

		<tr>
			<td style="background: url(/image/4WotsHot_side.gif) repeat-y;">
			<td style="padding:10px 0px">
				<table border="0">
					<tr>
						<td class="h5DarkBlue">Login </td>
						<td> <input maxlength="100" id="input_username" name="j_username" size="14" value="" ></td>
					</tr>
					<tr>
						<td class="h5DarkBlue">Password </td>
						<td> <input type="password" name="j_password" size="14"></td>
					</tr>
				</table>
			</td>
			<td style="background: url(/image/3WotsHot_side.gif) repeat-y;"/>
		</tr>
		<tr>
			<td style="background: url(/image/4WotsHot_side.gif) repeat-y;"/>
			<td align="center"><input type="image" src="/image/login_button.gif" alt="Log In" width="70" height="17" vspace="0" border="0"></td>
			<td style="background: url(/image/3WotsHot_side.gif) repeat-y;"/>
		</tr>
        <tr>
			<td style="background: url(/image/1WotsHot_bot.gif) no-repeat;height:10px;"/>
			<td style="background: url(/image/WotsHot_bot.gif) repeat-x;"/>
			<td style="background: url(/image/3WotsHot_bot.gif) no-repeat bottom right;"/>
		</tr>
	</table>
</form>
Unicode Text: <%="М м Б Ц ц" %>
Here are some symbols:  ↑ (&#8593;)
<p/>
Here are some Cyrillic characters:  М м Б Ц ц (&#1052; &#1084; &#1041; &#1062; &#1094;)
<p/>
Here are some Cyrillic characters: <%="М м Б Ц ц" %>
<jsp:include page="/include/Footer.jsp"/>