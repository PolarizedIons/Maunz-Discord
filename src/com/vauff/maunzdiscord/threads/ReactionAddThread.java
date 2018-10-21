package com.vauff.maunzdiscord.threads;

import com.vauff.maunzdiscord.core.AbstractCommand;
import com.vauff.maunzdiscord.core.AbstractMenuPage;
import com.vauff.maunzdiscord.core.Logger;
import com.vauff.maunzdiscord.core.Util;
import sx.blah.discord.handle.impl.events.guild.channel.message.reaction.ReactionAddEvent;
import sx.blah.discord.handle.impl.obj.ReactionEmoji;

import java.util.Random;

public class ReactionAddThread implements Runnable
{
	private ReactionAddEvent event;
	private Thread thread;
	private String name;

	public ReactionAddThread(ReactionAddEvent passedEvent, String passedName)
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
		System.out.println("RUNNING");
		try
		{
			if (AbstractCommand.AWAITED.containsKey(event.getMessage().getStringID()) && event.getUser().getStringID().equals(AbstractCommand.AWAITED.get(event.getMessage().getStringID()).getID()))
			{
				event.getMessage().delete();
				AbstractCommand.AWAITED.get(event.getMessage().getStringID()).getCommand().onReactionAdd(event);
			}

			else if (AbstractMenuPage.ACTIVE.containsKey(event.getUser().getLongID()))
			{
				if (event.getMessageID() == AbstractMenuPage.ACTIVE.get(event.getUser().getLongID()).menu.getLongID())
				{
					ReactionEmoji e = event.getReaction().getEmoji();
					int index;

					switch (e.toString())
					{
						case "❌":
							index = -1;
							break;
						case "1⃣":
							index = 0;
							break;
						case "2⃣":
							index = 1;
							break;
						case "3⃣":
							index = 2;
							break;
						case "4⃣":
							index = 3;
							break;
						case "5⃣":
							index = 4;
							break;
						case "6⃣":
							index = 5;
							break;
						case "7⃣":
							index = 6;
							break;
						case "8⃣":
							index = 7;
							break;
						case "9⃣":
							index = 8;
							break;
						default:
							Logger.log.warn("Emoji added that is not part of the menu. Awaiting new input.");
							return;
					}

					AbstractMenuPage.ACTIVE.get(event.getUser().getLongID()).onReacted(event, index);
				}
			}
		}
		catch (Exception e)
		{
			Random rnd = new Random();
			int code = 100000 + rnd.nextInt(900000);

			Util.msg(event.getChannel(), event.getAuthor(), ":exclamation:  |  **Uh oh, an error occured!**" + System.lineSeparator() + System.lineSeparator() + "If this was an unexpected error, please report it to Vauff in the #bugreports channel at http://discord.gg/MDx3sMz with the error code " + code);
			Logger.log.error(code, e);
		}
		System.out.println("DONE");
	}
}
