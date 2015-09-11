<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
	<head>
		<title>CaRo Web -- converter for COMBINE archives and Research Objects</title>
		<style type="text/css">
			.error
			{
				color: #EC3315;
			}
		</style>
	</head>
	<body>
		<h1>CaRo Web</h1>
		
		<p>This web application is a converter for COMBINE archives and Research Objects</p>
		
		
	<c:if test="${not empty error}">
		<p class="error">
			<strong>ERROR:</strong> ${error}
		</p>
	</c:if>
		
		<p>For more information on CaRo and CaRoWeb please go to <a href="https://sems.uni-rostock.de/projects/caro/">the sems website</a></p>
		
	</body>
</html>