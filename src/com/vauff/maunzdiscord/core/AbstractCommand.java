package com.vauff.maunzdiscord.core;

import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.reaction.ReactionAddEvent;

import java.util.HashMap;

public abstract class AbstractCommand<M extends MessageReceivedEvent>
{
	/**
	 * Holds all messages as keys which await a reaction or reply by a specific user.
	 * The values hold an instance of {@link Await}
	 */

	public static final HashMap<String, Await> AWAITED = new HashMap<String, Await>();

	/**
	 * Executes this command
	 *
	 * @param event The event by which this command got triggered
	 * @throws Exception If an exception gets thrown by any implementing methods
	 */
	public abstract void exe(M event) throws Exception;

	/**
	 * Defines aliases that can be used to trigger the command.
	 * The main alias should also be defined in here
	 *
	 * @return A string array of all valid aliases
	 */
	public abstract String[] getAliases();

	/**
	 * Sets up this command to await a reaction by the user who triggered this command
	 *
	 * @param messageID The message which should get reacted on
	 * @param userID    The user who triggered this command
	 */
	public final void waitForReaction(String messageID, String userID)
	{
		AWAITED.put(messageID, new Await(userID, this));
	}

	/**
	 * Sets up this command to await a reply by the user who triggered this command
	 *
	 * @param messageID The message which will get deleted afterwards
	 * @param userID    The user who triggered this command
	 */
	public final void waitForReply(String messageID, String userID)
	{
		AWAITED.put(userID, new Await(messageID, this));
	}

	/**
	 * Defines if the default implementation of {@link AbstractCommand#onReactionAdd(ReactionAddEvent)}
	 *
	 * @return true if the default behavior of said method should be used, false otherwise
	 */
	public boolean confirmable()
	{
		return false;
	}

	/**
	 * Gets called when a reaction is added to a message defined prior in {@link AbstractCommand#waitForReaction(String, String)}
	 *
	 * @param event The event holding information about the added reaction
	 */
	public void onReactionAdd(ReactionAddEvent event) throws Exception
	{
	}

	/**
	 * Gets called when a specific user sends a reply defined prior in {@link AbstractCommand#waitForReply(String, String)}
	 *
	 * @param event The event holding information about the reply
	 */
	public void onMessageReceived(MessageReceivedEvent event) throws Exception
	{
	}
}
