package com.vauff.maunzdiscord.threads;

import com.github.koraktor.steamcondenser.servers.SourceServer;
import com.github.koraktor.steamcondenser.servers.SteamPlayer;
import com.vauff.maunzdiscord.core.Logger;
import com.vauff.maunzdiscord.core.Main;
import com.vauff.maunzdiscord.timers.ServerTimer;
import org.bson.Document;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static com.mongodb.client.model.Filters.eq;

public class ServerRequestThread implements Runnable
{
	private Document doc;
	private SourceServer server;
	private Thread thread;
	private String id;

	public ServerRequestThread(Document doc, String id)
	{
		this.doc = doc;
		this.id = id;
	}

	public void start()
	{
		if (thread == null)
		{
			thread = new Thread(this, "servertracking-" + id);
			thread.start();
		}
	}

	public void run()
	{
		try
		{
			int attempts = 0;

			while (true)
			{
				try
				{
					server = new SourceServer(InetAddress.getByName(doc.getString("ip")), doc.getInteger("port"));
					server.initialize();

					try
					{
						Main.mongoDatabase.getCollection("servers").updateOne(eq("_id", doc.getObjectId("_id")), new Document("$set", new Document("players", server.getPlayers().keySet())));
					}
					catch (NullPointerException e)
					{
						Set<String> keySet = new HashSet<>();

						for (SteamPlayer player : new ArrayList<>(server.getPlayers().values()))
						{
							keySet.add(player.getName());
						}

						Main.mongoDatabase.getCollection("servers").updateOne(eq("_id", doc.getObjectId("_id")), new Document("$set", new Document("players", keySet)));
					}

					break;
				}
				catch (Exception e)
				{
					attempts++;

					if (attempts >= 5 || doc.getInteger("downtimeTimer") >= doc.getInteger("failedConnectionsThreshold"))
					{
						Logger.log.warn("Failed to connect to the server " + doc.getString("ip") + ":" + doc.getInteger("port") + ", automatically retrying in 1 minute");
						int downtimeTimer = doc.getInteger("downtimeTimer") + 1;
						Main.mongoDatabase.getCollection("servers").updateOne(eq("_id", doc.getObjectId("_id")), new Document("$set", new Document("downtimeTimer", downtimeTimer)));

						if (downtimeTimer >= 10080)
						{
							Main.mongoDatabase.getCollection("servers").updateOne(eq("_id", doc.getObjectId("_id")), new Document("$set", new Document("enabled", false)));
						}

						cleanup(true);
						return;
					}
					else
					{
						Thread.sleep(1000);
					}
				}
			}

			HashMap<String, Object> serverInfo = server.getServerInfo();
			long timestamp = 0;
			String map = serverInfo.get("mapName").toString();
			String name = serverInfo.get("serverName").toString();
			int currentPlayers = Integer.parseInt(serverInfo.get("numberOfPlayers").toString());
			int maxPlayers = Integer.parseInt(serverInfo.get("maxPlayers").toString());

			if (currentPlayers > maxPlayers)
			{
				currentPlayers = maxPlayers;
			}

			String playerCount = currentPlayers + "/" + maxPlayers;

			if (!map.equals("") && !doc.getString("map").equalsIgnoreCase(map))
			{
				timestamp = System.currentTimeMillis();

				boolean mapFound = false;

				for (int i = 0; i < doc.getList("mapDatabase", Document.class).size(); i++)
				{
					String dbMap = doc.getList("mapDatabase", Document.class).get(i).getString("map");

					if (dbMap.equalsIgnoreCase(map))
					{
						mapFound = true;
						Main.mongoDatabase.getCollection("servers").updateOne(eq("_id", doc.getObjectId("_id")), new Document("$set", new Document("mapDatabase." + i + ".lastPlayed", timestamp)));
						break;
					}
				}

				if (!mapFound)
				{
					Document mapDoc = new Document();
					mapDoc.put("map", map);
					mapDoc.put("firstPlayed", timestamp);
					mapDoc.put("lastPlayed", timestamp);
					Main.mongoDatabase.getCollection("servers").updateOne(eq("_id", doc.getObjectId("_id")), new Document("$push", new Document("mapDatabase", mapDoc)));

				}
			}

			if (!map.equals(""))
				Main.mongoDatabase.getCollection("servers").updateOne(eq("_id", doc.getObjectId("_id")), new Document("$set", new Document("map", map)));

			if (!playerCount.equals(""))
				Main.mongoDatabase.getCollection("servers").updateOne(eq("_id", doc.getObjectId("_id")), new Document("$set", new Document("playerCount", playerCount)));

			if (timestamp != 0)
				Main.mongoDatabase.getCollection("servers").updateOne(eq("_id", doc.getObjectId("_id")), new Document("$set", new Document("timestamp", timestamp)));

			if (!name.equals(""))
				Main.mongoDatabase.getCollection("servers").updateOne(eq("_id", doc.getObjectId("_id")), new Document("$set", new Document("name", name)));

			Main.mongoDatabase.getCollection("servers").updateOne(eq("_id", doc.getObjectId("_id")), new Document("$set", new Document("downtimeTimer", 0)));
			cleanup(true);
		}
		catch (Exception e)
		{
			Logger.log.error("", e);
			cleanup(false);
		}
		finally
		{
			ServerTimer.threadRunning.put(id, false);
		}
	}

	private void cleanup(boolean success)
	{
		if (!Objects.isNull(server))
			server.disconnect();

		List<ServiceProcessThread> processThreads = new ArrayList<>(ServerTimer.waitingProcessThreads.get(doc.getObjectId("_id").toString()));

		if (success)
		{
			for (ServiceProcessThread processThread : processThreads)
				processThread.start();
		}

		ServerTimer.waitingProcessThreads.get(doc.getObjectId("_id").toString()).clear();
	}
}
