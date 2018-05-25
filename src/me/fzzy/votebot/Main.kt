package me.fzzy.votebot

import sx.blah.discord.api.ClientBuilder
import sx.blah.discord.api.IDiscordClient

lateinit var cli: IDiscordClient
lateinit var guilds: ArrayList<Leaderboard>

var running = false

fun main(args: Array<String>) {
    if (args.size != 1) {
        println("Please enter the bots tokens e.g. java -jar thisjar.jar tokenhere")
        return
    }
    running = true

    guilds = ArrayList()

    Task().start()

    cli = ClientBuilder().withToken(args[0]).build()
    cli.dispatcher.registerListener(Events())
    cli.login()
}

fun getLeaderboard(guildId: Long): Leaderboard? {
    for (leaderboard in guilds) {
        if (leaderboard.leaderboardGuildId == guildId)
            return leaderboard
    }
    return null
}