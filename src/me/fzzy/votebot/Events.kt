package me.fzzy.votebot

import sx.blah.discord.api.events.EventSubscriber
import sx.blah.discord.handle.impl.events.ReadyEvent
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import sx.blah.discord.handle.impl.events.guild.channel.message.reaction.ReactionAddEvent
import sx.blah.discord.handle.impl.events.guild.channel.message.reaction.ReactionRemoveEvent
import sx.blah.discord.handle.impl.obj.ReactionEmoji
import sx.blah.discord.handle.obj.ActivityType
import sx.blah.discord.handle.obj.IChannel
import sx.blah.discord.handle.obj.IMessage
import sx.blah.discord.handle.obj.StatusType
import sx.blah.discord.util.MissingPermissionsException
import sx.blah.discord.util.RequestBuffer
import sx.blah.discord.util.RequestBuilder
import java.util.regex.Matcher
import java.util.regex.Pattern
import sx.blah.discord.util.audio.AudioPlayer
import java.io.File

const val FZZY_ID = 66104132028604416L

class Events {

    @EventSubscriber
    fun onMessageReceived(event: MessageReceivedEvent) {
        if (!event.message.author.isBot) {
            if (getLeaderboard(event.guild.longID) == null)
                guilds.add(Leaderboard(event.guild.longID))
            val m: Matcher = Pattern.compile("^((https?|ftp)://|(www|ftp)\\.)?[a-z0-9-]+(\\.[a-z0-9-]+)+([/?].*)?$").matcher(event.message.content)
            if (m.find() || event.message.attachments.size > 0) {
                RequestBuilder(event.client).shouldBufferRequests(true).doAction {
                    try {
                        event.message.addReaction(ReactionEmoji.of("upvote", 445376322353496064L))
                    } catch (e: MissingPermissionsException) {

                    }
                    true
                }.andThen {
                    try {
                        event.message.addReaction(ReactionEmoji.of("downvote", 445376330989830147L))
                    } catch (e: MissingPermissionsException) {

                    }
                    true
                }.execute()
            }
            // Stop the bot
            if (event.message.content.equals("-stop", true)) {
                if (event.author.longID == FZZY_ID) {
                    for (leaderboard in guilds) {
                        leaderboard.saveLeaderboard()
                    }
                    cli.logout()
                    running = false
                    System.exit(0)
                }
            }
        }
    }

    @EventSubscriber
    fun onReactionAdd(event: ReactionAddEvent) {
        val leaderboard = getLeaderboard(event.guild.longID)
        if (leaderboard != null) {
            if (System.currentTimeMillis() / 1000 - event.message.timestamp.epochSecond < 60 * 60 * 24) {
                if (event.reaction.getUserReacted(cli.ourUser)) {
                    if (event.message.author.longID != event.user.longID) {
                        when (event.reaction.emoji.name) {
                            "upvote" -> leaderboard.addToScore(event.author.longID, 1)
                            "downvote" -> leaderboard.addToScore(event.author.longID, -1)
                        }
                    }
                }
            }
        }
    }

    @EventSubscriber
    fun onReactionRemove(event: ReactionRemoveEvent) {
        val leaderboard = getLeaderboard(event.guild.longID)
        if (leaderboard != null) {
            if (System.currentTimeMillis() / 1000 - event.message.timestamp.epochSecond < 60 * 60 * 24) {
                if (event.reaction.getUserReacted(cli.ourUser)) {
                    if (event.message.author.longID != event.user.longID) {
                        when (event.reaction.emoji.name) {
                            "upvote" -> leaderboard.addToScore(event.author.longID, -1)
                            "downvote" -> leaderboard.addToScore(event.author.longID, 1)
                        }
                    }
                }
            }
        }
    }

    @EventSubscriber
    fun onReady(event: ReadyEvent) {
        for (guild in cli.guilds) {
            val leaderboard = Leaderboard(guild.longID)
            guilds.add(leaderboard)
            leaderboard.loadLeaderboard()
            leaderboard.updateLeaderboard()
        }
        RequestBuffer.request { cli.changePresence(StatusType.ONLINE, ActivityType.PLAYING, "discord.gg/jXcjUyf") }
    }
}
