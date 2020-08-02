package com.vauff.maunzmigrator;

import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.apache.commons.io.FileUtils;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

public class Main
{
	public static void main(String[] args) throws Exception
	{
		File serverTrackingFolder = new File(getJarLocation() + "data/services/server-tracking/");
		JSONObject configJson = new JSONObject(FileUtils.readFileToString(new File(getJarLocation() + "config.json"), "UTF-8"));
		MongoDatabase mongoDatabase = MongoClients.create(configJson.getJSONObject("mongoDatabase").getString("connectionString")).getDatabase(configJson.getJSONObject("mongoDatabase").getString("database"));

		for (File rootFolder : serverTrackingFolder.listFiles())
		{
			HashMap<String, ObjectId> convertedServerDictionary = new HashMap<>();

			System.out.println("Converting " + rootFolder.getName());

			JSONObject json = new JSONObject(FileUtils.readFileToString(new File(getJarLocation() + "data/services/server-tracking/" + rootFolder.getName() + "/serverInfo.json"), "UTF-8"));
			int serverNumber = 0;

			while (true)
			{
				JSONObject object;

				try
				{
					object = json.getJSONObject("server" + serverNumber);
				}
				catch (JSONException e)
				{
					break;
				}

				serverNumber++;

				Document serverDoc = new Document();
				boolean serverExists = false;

				for (Document doc : mongoDatabase.getCollection("servers").find(and(eq("ip", object.getString("serverIP")), eq("port", object.getInt("serverPort")))))
				{
					serverDoc = doc;
					serverExists = true;
					break;
				}

				if (!serverExists)
				{
					serverDoc.put("enabled", object.getBoolean("enabled"));
					serverDoc.put("ip", object.getString("serverIP"));
					serverDoc.put("port", object.getInt("serverPort"));
					serverDoc.put("name", object.getString("serverName"));
					serverDoc.put("map", object.getString("lastMap"));
					serverDoc.put("timestamp", object.getLong("timestamp"));
					serverDoc.put("playerCount", object.getString("players"));
					serverDoc.put("players", new ArrayList<>());
					serverDoc.put("downtimeTimer", object.getInt("downtimeTimer"));
					serverDoc.put("failedConnectionsThreshold", 3);

					ArrayList<Document> databaseEntries = new ArrayList<>();

					for (int i = 0; i < object.getJSONArray("mapDatabase").length(); i++)
					{
						JSONObject databaseEntryJson = object.getJSONArray("mapDatabase").getJSONObject(i);
						Document databaseEntry = new Document();

						databaseEntry.put("map", databaseEntryJson.getString("mapName"));
						databaseEntry.put("firstPlayed", databaseEntryJson.getLong("firstPlayed"));
						databaseEntry.put("lastPlayed", databaseEntryJson.getLong("lastPlayed"));

						databaseEntries.add(databaseEntry);
					}

					serverDoc.put("mapDatabase", databaseEntries);

					mongoDatabase.getCollection("servers").insertOne(serverDoc);
				}

				Document serviceDoc = new Document();

				serviceDoc.put("enabled", object.getBoolean("enabled"));
				serviceDoc.put("online", object.getInt("downtimeTimer") < object.getInt("failedConnectionsThreshold"));
				serviceDoc.put("mapCharacterLimit", object.getBoolean("mapCharacterLimit"));
				serviceDoc.put("lastMap", object.getString("lastMap"));
				serviceDoc.put("serverID", mongoDatabase.getCollection("servers").find(and(eq("ip", object.getString("serverIP")), eq("port", object.getInt("serverPort")))).first().getObjectId("_id"));
				serviceDoc.put("guildID", Long.parseLong(rootFolder.getName()));
				serviceDoc.put("channelID", object.getLong("serverTrackingChannelID"));
				serviceDoc.put("notifications", new ArrayList<>());

				convertedServerDictionary.put("server" + serverNumber, mongoDatabase.getCollection("services").insertOne(serviceDoc).getInsertedId().asObjectId().getValue());
			}

			for (File file : rootFolder.listFiles())
			{
				if (!file.getName().equals("serverInfo.json"))
				{
					json = new JSONObject(FileUtils.readFileToString(file, "UTF-8"));
					serverNumber = 0;

					while (true)
					{
						JSONArray array;

						try
						{
							array = json.getJSONObject("notifications").getJSONArray("server" + serverNumber);
						}
						catch (JSONException e)
						{
							break;
						}

						serverNumber++;

						Document serviceDoc = mongoDatabase.getCollection("services").find(and(eq("guildID", Long.parseLong(rootFolder.getName())), eq("_id", convertedServerDictionary.get("server" + serverNumber)))).first();
						Document notificationDoc = new Document();
						ArrayList<String> notifications = new ArrayList<>();

						for (int i = 0; i < array.length(); i++)
						{
							if (!array.isNull(i))
								notifications.add(array.getString(i));
						}

						if (notifications.size() == 0)
							continue;

						notificationDoc.put("userID", Long.parseLong(file.getName().replace(".json", "")));
						notificationDoc.put("notifications", notifications);
						serviceDoc.getList("notifications", Document.class).add(notificationDoc);

						mongoDatabase.getCollection("services").replaceOne(eq("_id", serviceDoc.getObjectId("_id")), serviceDoc);
					}
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
