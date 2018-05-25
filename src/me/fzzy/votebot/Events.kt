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
            // Help command
            if (event.message.content.equals("-votebot help", true)) {
                RequestBuilder(event.client).shouldBufferRequests(true).doAction {
                    event.author.orCreatePMChannel.sendMessage("**Bot Setup**\n" +
                                    "**1)** Simply adding the bot to your server will allow it to add the proper reactions to any message as long as it has permission. " +
                                    "(if you would like to disable it in a particular channel you can just revoke its access to add reactions in that channel)\n" +
                                    "**2)** To set up the leaderboard all you need to do is create a text channel called \"leaderboard\" and give the bot permission to send messages in the channel. " +
                                    "After that you need to remove permission from other users to send messages in the leaderboard channel. " +
                                    "Only Vote Bot should be able to send messages in this channel."
                    )
                    true
                }.andThen {
                    event.author.orCreatePMChannel.sendFile(File("directions.jpg"))
                    true
                }.andThen {
                    event.author.orCreatePMChannel.sendFile(File("directions2.jpg"))
                    true
                }.andThen {
                    event.author.orCreatePMChannel.sendMessage("If you need additional help or have questions you can join the help discord at https://discord.gg/jXcjUyf")
                    true
                }.execute()
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
        RequestBuffer.request { cli.changePresence(StatusType.ONLINE, ActivityType.PLAYING, "-votebot help") }
    }
}
