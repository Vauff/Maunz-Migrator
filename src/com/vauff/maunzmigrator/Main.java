package com.vauff.maunzmigrator;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;

public class Main
{
	public static void main(String[] args) throws Exception
	{
		File configFile = new File(getJarLocation() + "config.json");
		File serverTrackingFolder = new File(getJarLocation() + "data/services/server-tracking/");
		JSONObject configJson = new JSONObject(FileUtils.readFileToString(configFile, "UTF-8"));

		if (configJson.has("cleverbotAPIKey"))
		{
			configJson.remove("cleverbotAPIKey");
		}

		FileUtils.writeStringToFile(configFile, configJson.toString(2), "UTF-8");

		for (File rootFolder : serverTrackingFolder.listFiles())
		{
			for (File file : rootFolder.listFiles())
			{
				if (file.getName().equals("serverInfo.json"))
				{
					JSONObject originalJson = new JSONObject(FileUtils.readFileToString(file, "UTF-8"));
					JSONObject newJson = new JSONObject();

					newJson.put("server0", new JSONObject());
					newJson.put("lastGuildName", originalJson.getString("lastGuildName"));
					newJson.getJSONObject("server0").put("failedConnectionsThreshold", originalJson.getInt("failedConnectionsThreshold"));
					newJson.getJSONObject("server0").put("downtimeTimer", originalJson.getInt("downtimeTimer"));
					newJson.getJSONObject("server0").put("players", originalJson.getString("players"));
					newJson.getJSONObject("server0").put("lastMap", originalJson.getString("lastMap"));
					newJson.getJSONObject("server0").put("mapCharacterLimit", originalJson.getBoolean("mapCharacterLimit"));
					newJson.getJSONObject("server0").put("serverIP", originalJson.getString("serverIP"));
					newJson.getJSONObject("server0").put("serverTrackingChannelID", originalJson.getLong("serverTrackingChannelID"));
					newJson.getJSONObject("server0").put("serverPort", originalJson.getInt("serverPort"));
					newJson.getJSONObject("server0").put("enabled", originalJson.getBoolean("enabled"));
					newJson.getJSONObject("server0").put("timestamp", originalJson.getLong("timestamp"));
					newJson.getJSONObject("server0").put("mapDatabase", new JSONArray());

					if (originalJson.has("serverName"))
					{
						newJson.getJSONObject("server0").put("serverName", originalJson.getString("serverName"));
					}
					else
					{
						newJson.getJSONObject("server0").put("serverName", "");
					}

					for (int i = 0; i < originalJson.getJSONArray("mapDatabase").length(); i++)
					{
						String map = originalJson.getJSONArray("mapDatabase").getString(i);
						JSONObject databaseEntry = new JSONObject();

						databaseEntry.put("firstPlayed", 0L);
						databaseEntry.put("mapName", map);
						databaseEntry.put("lastPlayed", 0L);
						newJson.getJSONObject("server0").append("mapDatabase", databaseEntry);
					}

					FileUtils.writeStringToFile(file, newJson.toString(2), "UTF-8");
				}
				else
				{
					JSONObject originalJson = new JSONObject(FileUtils.readFileToString(file, "UTF-8"));
					JSONObject newJson = new JSONObject();

					newJson.put("lastName", originalJson.getString("lastName"));
					newJson.put("notifications", new JSONObject().put("server0", new JSONArray()));

					for (int i = 0; i < originalJson.getJSONArray("notifications").length(); i++)
					{
						newJson.getJSONObject("notifications").getJSONArray("server0").put(originalJson.getJSONArray("notifications").getString(i));
					}

					FileUtils.writeStringToFile(file, newJson.toString(2), "UTF-8");
				}
			}
		}
	}

	public static String getJarLocation() throws Exception
	{
		String path = Main.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();

		if (path.endsWith(".jar"))
		{
			path = path.substring(0, path.lastIndexOf("/"));
		}

		if (!path.endsWith("/"))
		{
			path += "/";
		}

		return path;
	}
}
