<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>Insert title here</title>
</head>
<body>
	<h2>get/post 방식으로 전송된 폼값 받기</h2>
	<%
	request.setCharacterEncoding("UTF-8");
	
	String id = request.getParameter("user_id");
	String pw = request.getParameter("user_pwd");
	%>
	<ul>
		<li>전송된 아이디 : <%=id %></li>
		<li>전송된 패스워드 : <%=pw %></li>
	</ul>
</body>
</html>