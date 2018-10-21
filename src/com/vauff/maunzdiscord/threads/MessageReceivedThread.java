package com.vauff.maunzdiscord.threads;

import com.vauff.maunzdiscord.commands.Disable;
import com.vauff.maunzdiscord.commands.Enable;
import com.vauff.maunzdiscord.core.AbstractCommand;
import com.vauff.maunzdiscord.core.Logger;
import com.vauff.maunzdiscord.core.Main;
import com.vauff.maunzdiscord.core.MainListener;
import com.vauff.maunzdiscord.core.Util;
import org.json.JSONObject;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;

import java.io.File;
import java.util.Random;

public class MessageReceivedThread implements Runnable
{
	private MessageReceivedEvent event;
	private Thread thread;
	private String name;

	public MessageReceivedThread(MessageReceivedEvent passedEvent, String passedName)
	{
		name = passedName;
		event = passedEvent;
	}

	public void start()
	{
		if (thread == null)
		{
			thread = new Thread(this, name);
			thread.start();
		}
	}

	public void run()
	{
		System.out.println("msg recv start");
		try
		{
			String cmdName = event.getMessage().getContent().split(" ")[0];

			for (AbstractCommand<MessageReceivedEvent> cmd : MainListener.commands)
			{
				for (String s : cmd.getAliases())
				{
					if (cmdName.equalsIgnoreCase(s))
					{
						boolean enabled;

						if (event.getChannel().isPrivate())
						{
							enabled = Util.isEnabled();
						}
						else
						{
							enabled = Util.isEnabled(event.getGuild());
						}

						if (enabled || cmd instanceof Enable || cmd instanceof Disable)
						{
							if (MainListener.cooldownTimestamps.containsKey(event.getAuthor().getStringID()) && (MainListener.cooldownTimestamps.get(event.getAuthor().getStringID()) + 2000L) > System.currentTimeMillis())
							{
								if (MainListener.cooldownMessageTimestamps.containsKey(event.getAuthor().getStringID()) && (MainListener.cooldownMessageTimestamps.get(event.getAuthor().getStringID()) + 10000L) < System.currentTimeMillis())
								{
									Util.msg(event.getChannel(), event.getAuthor(), event.getAuthor().mention() + " Slow down!");
									MainListener.cooldownMessageTimestamps.put(event.getAuthor().getStringID(), System.currentTimeMillis());
								}
								else if (!MainListener.cooldownMessageTimestamps.containsKey(event.getAuthor().getStringID()))
								{
									Util.msg(event.getChannel(), event.getAuthor(), event.getAuthor().mention() + " Slow down!");
									MainListener.cooldownMessageTimestamps.put(event.getAuthor().getStringID(), System.currentTimeMillis());
								}

								return;
							}

							MainListener.cooldownTimestamps.put(event.getAuthor().getStringID(), System.currentTimeMillis());
							boolean blacklisted = false;

							if (!Util.hasPermission(event.getAuthor(), event.getGuild()) && !event.getChannel().isPrivate())
							{
								JSONObject json = new JSONObject(Util.getFileContents(new File(Util.getJarLocation() + "data/guilds/" + event.getGuild().getStringID() + ".json")));

								for (int i = 0; i < json.getJSONArray("blacklist").length(); i++)
								{
									String entry = json.getJSONArray("blacklist").getString(i);

									if ((entry.split(":")[0].equalsIgnoreCase(event.getChannel().getStringID()) || entry.split(":")[0].equalsIgnoreCase("all")) && (entry.split(":")[1].equalsIgnoreCase(cmdName.replace("*", "")) || entry.split(":")[1].equalsIgnoreCase("all")))
									{
										blacklisted = true;
										break;
									}
								}
							}

							if (!blacklisted)
							{
								event.getChannel().setTypingStatus(true);
								Thread.sleep(10);

								try
								{
									cmd.exe(event);
								}
								catch (Exception e)
								{
									Random rnd = new Random();
									int code = 100000 + rnd.nextInt(900000);

									Util.msg(event.getChannel(), event.getAuthor(), ":exclamation:  |  **Uh oh, an error occured!**" + System.lineSeparator() + System.lineSeparator() + "If this was an unexpected error, please report it to Vauff in the #bugreports channel at http://discord.gg/MDx3sMz with the error code " + code);
									Logger.log.error(code, e);
								}

								event.getChannel().setTypingStatus(false);
							}
							else
							{
								Util.msg(event.getAuthor().getOrCreatePMChannel(), ":exclamation:  |  **Command/channel blacklisted**" + System.lineSeparator() + System.lineSeparator() + "The bot wasn't able to reply to your command in " + event.getChannel().mention() + " because a guild administrator has blacklisted either the command or the channel that you ran it in");
							}
						}
					}
				}
			}

			try
			{
				if (AbstractCommand.AWAITED.containsKey(event.getAuthor().getStringID()) && event.getChannel().equals(Main.client.getMessageByID(Long.parseLong(AbstractCommand.AWAITED.get(event.getAuthor().getStringID()).getID())).getChannel()))
				{
					Main.client.getMessageByID(Long.parseLong(AbstractCommand.AWAITED.get(event.getAuthor().getStringID()).getID())).delete();
					AbstractCommand.AWAITED.get(event.getAuthor().getStringID()).getCommand().onMessageReceived(event);
				}
			}
			catch (NullPointerException e)
			{
				// This means that the message ID in AbstractCommand#AWAITED for the given user ID has already been deleted, we can safely just stop executing
			}
		}
		catch (Exception e)
		{
			Logger.log.error("", e);
		}
		System.out.println("msg recv end");
	}
}
