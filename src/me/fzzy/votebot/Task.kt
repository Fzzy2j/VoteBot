package me.fzzy.votebot

import sx.blah.discord.handle.obj.ActivityType
import sx.blah.discord.handle.obj.StatusType
import sx.blah.discord.util.DiscordException
import sx.blah.discord.util.MissingPermissionsException
import sx.blah.discord.util.RequestBuffer
import java.util.*

class Task : Thread() {

    override fun run() {
        while (running) {
            Thread.sleep(60 * 1000)

            RequestBuffer.request { cli.changePresence(StatusType.ONLINE, ActivityType.PLAYING, "discord.gg/jXcjUyf") }
            println("auto-save for ${guilds.size} guilds")
            for (leaderboard in guilds) {
                if (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY) {
                    if (leaderboard.weekWinner == null)
                        leaderboard.weekWinner = leaderboard.getCurrentWinner()
                    if (leaderboard.weekWinner != null) {
                        if (System.currentTimeMillis() - leaderboard.weekWinner!!.timestamp > 1000 * 60 * 60 * 25 * 1) {
                            leaderboard.weekWinner = leaderboard.getCurrentWinner()
                            leaderboard.clearLeaderboard()
                        }
                    }
                }

                leaderboard.saveLeaderboard()
                try {
                    leaderboard.updateLeaderboard()
                } catch (e: MissingPermissionsException) {
                    println("Could not update leaderboard for guild")
                    e.printStackTrace()
                } catch (e: DiscordException) {
                    println("Could not update leaderboard for guild")
                    e.printStackTrace()
                } catch (e: NullPointerException) {
                    println("Could not update leaderboard for guild")
                    e.printStackTrace()
                }
            }
        }
    }

}