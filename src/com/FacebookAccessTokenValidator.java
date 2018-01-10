package com;

import java.net.*;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;

public class FacebookAccessTokenValidator {
	public static String getText(String url) {
		try {
			URL website = new URL(url);
			URLConnection connection = website.openConnection();
			BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			StringBuilder response = new StringBuilder();
			String inputLine;
			while ((inputLine = in.readLine()) != null)
				response.append(inputLine);
			in.close();
			return response.toString();
		} catch (Exception e) {
			return null;
		}
	}
	public static JSONObject getURLJson(String url) {
		String text = getText(url);
		JSONParser parser = new JSONParser();
		System.out.println("JSONParser");
		System.out.println("getURLJson "+url);
		System.out.println("text "+text);
		if(text != null) {
			try {
				Object obj = parser.parse(text);
	            JSONObject jsonObject = (JSONObject) obj;
				return jsonObject;
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	public static JSONObject getUserInfo(String fbAccessToken) {
		return getURLJson("https://graph.facebook.com/me?access_token=" + fbAccessToken);
	}

	public static JSONObject getUserFriends(String fbAccessToken) {
		return getURLJson("https://graph.facebook.com/me/friends?access_token=" + fbAccessToken);
	}
}
