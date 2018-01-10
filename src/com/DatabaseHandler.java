package com;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.json.simple.JSONObject;

public class DatabaseHandler {
	Connection con;

	public DatabaseHandler() {
		try {
			Class.forName("com.mysql.jdbc.Driver");
			con = DriverManager.getConnection("jdbc:mysql://", "", "");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public boolean userNameAvailable(String userName) {
		try {
			Statement stmt = con.createStatement();
			ResultSet set = stmt.executeQuery("SELECT * FROM user where userName='" + userName + "'");
			while (set.next()) {
				return false;
			}
			stmt.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
	}

	public void addFriend(String userId, String friendId) {
		try {
			Statement stmt = con.createStatement();
			String sql = "INSERT INTO friends (userId, friendId)" + "VALUES ('" + userId + "','" + friendId + "')";
			stmt.executeUpdate(sql);
			stmt.close();
		} catch (Exception e) {
			// e.printStackTrace();
		}
	}

	public void insertUser(String id, String userName, String name, String photoURL) {
		try {
			PreparedStatement stmt = con.prepareStatement(
					"INSERT INTO user (fbProfileId, userName, name, photoURL)" + "VALUES(?, ?, ?, ?)");
			stmt.setString(1, id);
			stmt.setString(2, userName);
			stmt.setString(3, name);
			stmt.setString(4, photoURL);
			stmt.executeUpdate();
			stmt.close();
		} catch (Exception e) {
			// e.printStackTrace();
		}
		
		
	}

	public User getUserInfo(String userName) {
		try {
			Statement stmt = con.createStatement();
			ResultSet set = stmt.executeQuery("SELECT * FROM user where userName='" + userName + "'");
			while (set.next()) {
				User user = new User();
				user.email = set.getString("email");
				user.firstName = set.getString("firstName");
				user.photoURL = set.getString("photoURL");
				user.userName = set.getString("userName");
				if (set.getString("isNotificationsOn").equals("Y")) {
					user.isNotificationsOn = true;
				} else {
					user.isNotificationsOn = false;
				}
				return user;
			}
			stmt.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public boolean sendMessage(String senderId, String receiverName, String message) {
		try {
			String receiverId = getUserId(receiverName);
			if (receiverId == null) {
				return false;
			}
			String messageId = getNewMessageId();
			PreparedStatement stmt = con.prepareStatement(
					"INSERT INTO message (messageId, senderId, receiverId, message)" + "VALUES(?, ?, ?, ?)");
			stmt.setString(1, messageId);
			stmt.setString(2, senderId);
			stmt.setString(3, receiverId);
			stmt.setString(4, message);
			stmt.executeUpdate();
			stmt.close();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	private String getNewMessageId() {
		String messageId = getSaltString();
		if (isMessageIdPresent(messageId)) {
			return getNewMessageId();
		}
		return messageId;
	}

	private boolean isMessageIdPresent(String messageId) {
		try {
			Statement stmt = con.createStatement();
			String sql = "SELECT * FROM message where messageId='" + messageId + "'";
			ResultSet set = stmt.executeQuery(sql);
			while (set.next()) {
				return false;
			}
			stmt.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	private String getUserId(String receiverName) {
		try {
			Statement stmt = con.createStatement();
			String sql = "SELECT fbProfileId FROM user where userName='" + receiverName + "'";
			ResultSet set = stmt.executeQuery(sql);
			String fbProfileId = null;
			while (set.next()) {
				fbProfileId = set.getString("fbProfileId");
			}
			stmt.close();
			return fbProfileId;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public String getSaltString() {
		String SALTCHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890";
		StringBuilder salt = new StringBuilder();
		Random rnd = new Random();
		while (salt.length() < 32) { // length of the random string.
			int index = (int) (rnd.nextFloat() * SALTCHARS.length());
			salt.append(SALTCHARS.charAt(index));
		}
		String saltStr = salt.toString();
		return saltStr;

	}

	public List<JSONObject> getUserFriends(String userId) {
		try {
			List<JSONObject> list = new ArrayList<JSONObject>();
			Statement stmt = con.createStatement();
			String sql = "SELECT fbProfileId, userName, name FROM user WHERE fbProfileId IN (SELECT friendId FROM friends WHERE userId='"
					+ userId + "')";
			ResultSet set = stmt.executeQuery(sql);
			while (set.next()) {
				JSONObject jsonObject = new JSONObject();
				jsonObject.put("id", set.getString("fbProfileId"));
				jsonObject.put("userName", set.getString("userName"));
				jsonObject.put("name", set.getString("name"));
				list.add(jsonObject);
			}
			stmt.close();
			return list;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public boolean checkUser(String user_name) {
		try {
			Statement stmt = con.createStatement();
			String sql = "SELECT * from user WHERE userName = '" + user_name + "'";
			ResultSet set = stmt.executeQuery(sql);
			while (set.next()) {
				return true;
			}
			stmt.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public boolean checkReceiverIdMessageId(String receiverId, String messageId) {
		try {
			Statement stmt = con.createStatement();
			String sql = "SELECT * FROM message where receiverId='" + receiverId + "' AND messageId='" + messageId
					+ "'";
			ResultSet set = stmt.executeQuery(sql);
			while (set.next()) {
				return true;
			}
			stmt.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public boolean checkSenderIdMessageId(String senderId, String messageId) {
		try {
			Statement stmt = con.createStatement();
			String sql = "SELECT * FROM message where senderId='" + senderId + "' AND messageId='" + messageId
					+ "'";
			ResultSet set = stmt.executeQuery(sql);
			while (set.next()) {
				return true;
			}
			stmt.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public boolean approveIdentity(String senderId, String messageId) {
		if (checkSenderIdMessageId(senderId, messageId)) {
			try {
				PreparedStatement stmt = con
						.prepareStatement("UPDATE message set isRequestAccepted = 'Y' WHERE messageId=? AND senderId=? AND isRequested='Y'");
				stmt.setString(1, messageId);
				stmt.setString(2, senderId);
				stmt.executeUpdate();
				stmt.close();
				return true;
			} catch (Exception e) {
				e.printStackTrace();
			}
			return false;
		}
		return false;
	}
	public boolean deleteMessage(String receiverId, String messageId) {
		if (checkReceiverIdMessageId(receiverId, messageId)) {
			try {
				String query = "UPDATE message set isHidden = 'Y' WHERE messageId=? AND receiverId=?";
				PreparedStatement stmt = con.prepareStatement(query);
				stmt.setString(1, messageId);
				stmt.setString(2, receiverId);
				stmt.executeUpdate();
				stmt.close();
				return true;
			} catch (Exception e) {
				e.printStackTrace();
			}
			return false;
		}
		return false;
	}
	public boolean markFavorites(String receiverId, String messageId) {
		if (checkReceiverIdMessageId(receiverId, messageId)) {
			boolean isFavorite = getIsFavorite(receiverId, messageId);
			try {
				String query = "UPDATE message set isFavorite = 'Y' WHERE messageId=? AND receiverId=?";
				if(isFavorite) {
					query = "UPDATE message set isFavorite = 'N' WHERE messageId=? AND receiverId=?";
				}
				PreparedStatement stmt = con.prepareStatement(query);
				stmt.setString(1, messageId);
				stmt.setString(2, receiverId);
				stmt.executeUpdate();
				stmt.close();
				return true;
			} catch (Exception e) {
				e.printStackTrace();
			}
			return false;
		}
		return false;
	}
	private boolean getIsFavorite(String receiverId, String messageId) {
		try {
			Statement stmt = con.createStatement();
			String sql = "SELECT * FROM message where receiverId='" + receiverId + "' AND messageId='" + messageId
					+ "' AND isFavorite='Y'";
			ResultSet set = stmt.executeQuery(sql);
			while (set.next()) {
				return true;
			}
			stmt.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public boolean requestIdentity(String receiverId, String messageId) {
		if (checkReceiverIdMessageId(receiverId, messageId)) {
			try {
				PreparedStatement stmt = con
						.prepareStatement("UPDATE message set isRequested = 'Y' WHERE messageId=? AND receiverId=?");
				stmt.setString(1, messageId);
				stmt.setString(2, receiverId);
				stmt.executeUpdate();
				stmt.close();
				return true;
			} catch (Exception e) {
				e.printStackTrace();
			}
			return false;
		}
		return false;
	}

	public List<Message> getReceivedMessage(String user, boolean isFavorite) {
		String query = "SELECT m.messageId, m.message, DATE_FORMAT(m.messageTS, '%D %M %Y %r') messageTS, m.senderId, u.userName, u.name, m.isRequested, m.isRequestAccepted, m.isFavorite FROM message m, user u where m.isHidden = 'N' AND m.receiverId = '"
				+ user + "' AND u.fbProfileId = m.senderId order by messageTS desc";
		List<Message> messages = new ArrayList<Message>();
		try {
			Statement stmt = con.createStatement();
			ResultSet set = stmt.executeQuery(query);
			while (set.next()) {
				Message message = new Message();
				message.messageId = set.getString("messageId");
				message.message = set.getString("message");
				message.messageTS = set.getString("messageTS");
				message.senderId = set.getString("senderId");
				message.senderUserName = set.getString("userName");
				message.senderName = set.getString("name");
				if (set.getString("isRequested").equals("Y")) {
					message.isRequested = true;
				} else {
					message.isRequested = false;
				}
				if (set.getString("isRequestAccepted").equals("Y")) {
					message.isRequestAccepted = true;
				} else {
					message.isRequestAccepted = false;
				}
				if (set.getString("isFavorite").equals("Y")) {
					message.isFavorite = true;
				} else {
					message.isFavorite = false;
				}
				if (isFavorite) {
					if (message.isFavorite) {
						messages.add(message);
					}
				} else {
					messages.add(message);
				}
			}
			stmt.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return messages;
	}

	public int getFriendsCount(String user) {
		return getCount("SELECT count(*) cnt FROM friends where userId = '"+user+"'");
	}

	public int getSentCount(String user) {
		return getCount("SELECT count(*) cnt FROM message where senderId = '"+user+"'");
	}

	public int getReceivedCount(String user) {
		return getCount("SELECT count(*) cnt FROM message where receiverId = '"+user+"' and isHidden='N'");
	}

	public int getFavoritesCount(String user) {
		return getCount("SELECT count(*) cnt FROM message where receiverId = '"+user+"' and isFavorite='Y' and isHidden='N'");
	}
	
	public int getCount(String query) {
		int count = 0;
		try {
			Statement stmt = con.createStatement();
			ResultSet set = stmt.executeQuery(query);
			while (set.next()) {
				count = set.getInt("cnt");
			}
			stmt.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return count;
	}
	public List<Message> getSentMessage(String user) {
		String query = "SELECT m.messageId, m.message, DATE_FORMAT(m.messageTS, '%D %M %Y %r') messageTS, m.receiverId, u.userName, u.name, m.isRequested, m.isRequestAccepted FROM message m, user u where m.senderId = '"
				+ user + "' AND u.fbProfileId = m.receiverId order by messageTS desc";
		List<Message> messages = new ArrayList<Message>();
		try {
			Statement stmt = con.createStatement();
			ResultSet set = stmt.executeQuery(query);
			while (set.next()) {
				Message message = new Message();
				message.messageId = set.getString("messageId");
				message.message = set.getString("message");
				message.messageTS = set.getString("messageTS");
				message.receiverId = set.getString("receiverId");
				message.receiverUserName = set.getString("userName");
				message.receiverName = set.getString("name");
				if (set.getString("isRequested").equals("Y")) {
					message.isRequested = true;
				} else {
					message.isRequested = false;
				}
				if (set.getString("isRequestAccepted").equals("Y")) {
					message.isRequestAccepted = true;
				} else {
					message.isRequestAccepted = false;
				}
				messages.add(message);
			}
			stmt.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return messages;
	}
}
