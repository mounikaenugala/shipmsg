
<%@ page import="org.json.simple.*"%>
<%@ page import="com.User"%>
<%@ page import="java.util.Arrays"%>
<%@ page import="java.util.List"%>
<%@ page import="java.util.ArrayList"%>
<%@ page import="java.util.Map"%>
<%@ page import="com.DatabaseHandler"%>
<%@ page import="com.Message"%>
<%@ page import="com.FacebookAccessTokenValidator"%>
<%@ page import="javax.servlet.*"%>
<%@ page import="javax.servlet.http.*"%>


<%@ page language="java" contentType="application/json; charset=UTF-8"
	pageEncoding="UTF-8"%>

<%
	HttpSession httpSession = request.getSession(false);
	DatabaseHandler databaseHandler = new DatabaseHandler();
	JSONObject obj = new JSONObject();
	String apival = request.getParameter("api");
	if (apival == null) {
		obj.put("status", false);
	} else {
		String profileId = null;
		String accessToken = null;
		boolean isUserLoggedIn = false;
		httpSession = request.getSession(false);
		if (httpSession != null) {
			profileId = (String) httpSession.getAttribute("ProfileId");
			if (profileId != null) {
				isUserLoggedIn = true;
			}
			accessToken = (String) httpSession.getAttribute("AccessToken");
		} else {
		}

		if (apival.equals("logoutfb")) {
			httpSession = request.getSession(false);
			if (httpSession != null) {
				httpSession.invalidate();
			}
		} else if (apival.equals("reregister")) {
			String fbAccessToken = request.getParameter("accessToken");
			if (fbAccessToken == null) {
				obj.put("status", false);
			} else {
				httpSession = request.getSession(false);
				if (httpSession != null) {
					httpSession.setAttribute("AccessToken", fbAccessToken);
				}
				obj.put("status", true);
			}
		} else if (apival.equals("registerfb")) {
			String fbAccessToken = request.getParameter("accessToken");
			if (fbAccessToken == null) {
				obj.put("status", false);
			} else {
				JSONObject jsonObject = FacebookAccessTokenValidator.getUserInfo(fbAccessToken);
				if (jsonObject == null) {
					obj.put("status", false);
					obj.put("request", "reauth");
				} else {
					obj.put("status", true);
					String id = (String) jsonObject.get("id");
					String name = (String) jsonObject.get("name");
					if ((isUserLoggedIn) && (profileId.equals(id))) {
					} else {
						httpSession = request.getSession(false);
						if (httpSession != null) {
							httpSession.invalidate();
						}
						httpSession = request.getSession();
						httpSession.setAttribute("ProfileId", id);
						httpSession.setAttribute("UserName", name);
						httpSession.setAttribute("AccessToken", fbAccessToken);
						String userName = name.replace(" ", "");
						if (databaseHandler.userNameAvailable(userName)) {
						} else {
							int append = 0;
							String localUserName = userName + append;
							while (!databaseHandler.userNameAvailable(localUserName)) {
								append++;
								localUserName = userName + append;
							}
							userName = localUserName;
						}
						databaseHandler.insertUser(id, userName, name, "");
					}
				}
			}
		} else if (apival.equals("usersessioncheck")) {
			obj.put("status", isUserLoggedIn);

		} else if (apival.equals("send")) {
			String sendTo = request.getParameter("sendTo");
			String messageToSend = request.getParameter("messageToSend");
			if ((sendTo == null) || (messageToSend == null) || (profileId == null)) {
				obj.put("status", false);
			} else {
				databaseHandler.sendMessage(profileId, sendTo.trim(), messageToSend);
				obj.put("status", true);
			}
		} else if (apival.equals("count")) {
			int favCount = databaseHandler.getFavoritesCount(profileId);
			obj.put("favCount", favCount);
			
			int friendsCount = databaseHandler.getFriendsCount(profileId);
			obj.put("friendsCount", friendsCount);
			
			int receivedCount = databaseHandler.getReceivedCount(profileId);
			obj.put("receivedCount", receivedCount);
			
			int sentCount = databaseHandler.getSentCount(profileId);
			obj.put("sentCount", sentCount);
			
			obj.put("status", true);
		} else if (apival.equals("friends")) {
			JSONObject jsonObject = FacebookAccessTokenValidator.getUserInfo(accessToken);
			if(jsonObject != null) {
				String id = (String) jsonObject.get("id");
				JSONObject friendsJsonObject = FacebookAccessTokenValidator.getUserFriends(accessToken);
				if(friendsJsonObject != null) {
					JSONArray friendsJSON = (JSONArray) friendsJsonObject.get("data");
					for (int i = 0; i < friendsJSON.size(); i++) {
						JSONObject friendObject = (JSONObject) friendsJSON.get(i);
						databaseHandler.addFriend(id, (String) friendObject.get("id"));
					}
					obj.put("status", true);
					obj.put("friends", databaseHandler.getUserFriends(id));
				}else {
					obj.put("status", false);
					obj.put("request", "reauth");
				}
			} else {
				obj.put("status", false);
				obj.put("request", "reauth");
			}
		} else if (isUserLoggedIn) {
			if (apival.equals("logout")) {
				httpSession = request.getSession(false);
				if (httpSession != null) {
					httpSession.invalidate();
				}
				obj.put("status", true);
			} else if (apival.equals("requestidentity")) {
				String messageId = request.getParameter("messageId");
				if ((messageId == null) || (profileId == null)) {
					obj.put("status", false);
				} else {
					databaseHandler.requestIdentity(profileId, messageId);
					obj.put("status", true);
				}
			} else if (apival.equals("markFavorites")) {
				String messageId = request.getParameter("messageId");
				if ((messageId == null) || (profileId == null)) {
					obj.put("status", false);
				} else {
					databaseHandler.markFavorites(profileId, messageId);
					obj.put("status", true);
				}
			} else if (apival.equals("deleteMessage")) {
				String messageId = request.getParameter("messageId");
				if ((messageId == null) || (profileId == null)) {
					obj.put("status", false);
				} else {
					databaseHandler.deleteMessage(profileId, messageId);
					obj.put("status", true);
				}
			} else if (apival.equals("approveidentity")) {
				String messageId = request.getParameter("messageId");
				if ((messageId == null) || (profileId == null)) {
					obj.put("status", false);
				} else {
					databaseHandler.approveIdentity(profileId, messageId);
					obj.put("status", true);
				}
			} else if (apival.equals("messages")) {
				String msgType = request.getParameter("msgType");
				if (msgType == null) {
					obj.put("status", false);
				} else {
					if (profileId != null) {
						List<JSONObject> messagesJSON = new ArrayList<JSONObject>();
						List<Message> messages = new ArrayList<Message>();
						if (msgType.equals("received")) {
							messages = databaseHandler.getReceivedMessage(profileId, false);
						} else if (msgType.equals("favorites")) {
							messages = databaseHandler.getReceivedMessage(profileId, true);
						} else if (msgType.equals("sent")) {
							messages = databaseHandler.getSentMessage(profileId);
						}
						for (Message message : messages) {
							JSONObject messageJSON = new JSONObject();
							messageJSON.put("messageId", message.messageId);
							messageJSON.put("message", message.message);
							messageJSON.put("messageTS", message.messageTS);
							messageJSON.put("isRequested", message.isRequested);
							messageJSON.put("isRequestAccepted", message.isRequestAccepted);
							messageJSON.put("isFavorite", message.isFavorite);
							if (msgType.equals("received")) {
								if(message.isRequested && message.isRequestAccepted) {
									messageJSON.put("senderId", message.senderId);
									messageJSON.put("senderUserName", message.senderUserName);
									messageJSON.put("senderName", message.senderName);
								}
							} else if (msgType.equals("favorites")) {
								if(message.isRequested && message.isRequestAccepted) {
									messageJSON.put("senderId", message.senderId);
									messageJSON.put("senderUserName", message.senderUserName);
									messageJSON.put("senderName", message.senderName);
								}
							} else if (msgType.equals("sent")) {
								messageJSON.put("receiverId", message.receiverId);
								messageJSON.put("receiverUserName", message.receiverUserName);
								messageJSON.put("receiverName", message.receiverName);
							}
							messagesJSON.add(messageJSON);
						}
						obj.put("data", messagesJSON);
						obj.put("status", true);
					} else {
						obj.put("status", true);
						obj.put("msg", "No User Session");
					}
				}
			}
		}
	}
	response.setContentType("application/json");
	out.print(obj);
%>