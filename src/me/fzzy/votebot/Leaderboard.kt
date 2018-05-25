package me.fzzy.votebot

import sx.blah.discord.handle.obj.IChannel
import sx.blah.discord.util.EmbedBuilder
import sx.blah.discord.util.MissingPermissionsException
import sx.blah.discord.util.RequestBuffer
import java.io.File
import java.util.ArrayList
import java.util.LinkedHashMap

class Leaderboard constructor(private var guildId: Long) {

    private var scores: HashMap<Long, Int>
    var weekWinner: Winner? = null

    init {
        scores = hashMapOf()
    }

    val leaderboardGuildId: Long get() = this.guildId
    private val file: File = File("$guildId.txt")

    fun addToScore(id: Long, amt: Int) {
        scores[id] = scores.getOrDefault(id, 0) + amt
    }

    fun clearLeaderboard() {
        scores = hashMapOf()
    }

    fun saveLeaderboard() {
        if (scores.size > 0) {
            var serial = ""

            for ((key, value) in scores) {
                serial += ";$key,$value,"
            }

            serial += "%"
            if (weekWinner != null) {
                serial += "${weekWinner?.id},${weekWinner?.score},${weekWinner?.timestamp},"
            }

            file.printWriter().use { out -> out.println(serial.substring(1)) }
        } else
            file.printWriter().use { out -> out.println() }
    }

    fun loadLeaderboard() {
        scores = hashMapOf()
        if (file.exists()) {
            val serial = file.readText()
            if (serial.split("%")[0].split(";")[0].length > 2) {
                for (score in serial.split("%")[0].split(";")) {
                    val id = score.split(",")[0].toLong()
                    val value = score.split(",")[1].toInt()
                    scores[id] = value
                }
            }
            if (serial.split("%").size > 1) {
                val weekWinnerSerial = serial.split("%")[1]
                if (weekWinnerSerial.contains(",")) {
                    val id = weekWinnerSerial.split(",")[0].toLong()
                    val score = weekWinnerSerial.split(",")[1].toInt()
                    val timestamp = weekWinnerSerial.split(",")[2].toLong()
                    weekWinner = Winner(id, score, timestamp)
                }
            }
        }
    }

    fun updateLeaderboard() {
        val channel: MutableList<IChannel> = cli.getGuildByID(guildId).getChannelsByName("leaderboard")
        if (channel.size > 0) {
            val builder = EmbedBuilder()

            var count = 0
            for ((key, value) in getSortedLeaderboard()) {
                if (++count > 25)
                    break
                val title = "#$count - ${cli.getUserByID(key).getDisplayName(cli.getGuildByID(guildId))}"
                val description = "$value points"
                builder.appendField(title, description, false)
            }
            if (weekWinner != null) {
                builder.withTitle(":zap: ${cli.getUserByID(weekWinner!!.id).getDisplayName(cli.getGuildByID(guildId))} had the most points last week! :zap:")
                builder.withDescription(":zap: They had ${weekWinner!!.score} points! :zap:")
            }

            builder.withAuthorName("LEADERBOARD")
            builder.withAuthorIcon("http://i.imgur.com/dYhgv64.jpg")

            builder.withColor(0, 200, 255)
            builder.withThumbnail("https://i.gyazo.com/5227ef31b9cdbc11d9f1e7313872f4af.gif")

            if (channel[0].getMessageHistory(1).size > 0 && channel[0].getMessageHistory(1)[0].author.longID == cli.ourUser.longID)
                RequestBuffer.request { channel[0].getMessageHistory(1)[0].edit(builder.build()) }
            else
                RequestBuffer.request {
                    try {
                        channel[0].sendMessage(builder.build())
                    } catch (e: MissingPermissionsException) {
                        // No permission to send message
                    }
                }
        }
    }

    fun getCurrentWinner(): Winner? {
        for ((key, value) in getSortedLeaderboard()) {
            return Winner(key, value, System.currentTimeMillis())
        }
        return null
    }

    fun getSortedLeaderboard(): LinkedHashMap<Long, Int> {
        val mapKeys = ArrayList(scores.keys)
        val mapValues = ArrayList(scores.values)
        mapValues.sortDescending()
        mapKeys.sort()

        val sortedMap = LinkedHashMap<Long, Int>()

        val valueIt = mapValues.iterator()
        while (valueIt.hasNext()) {
            val value = valueIt.next()
            val keyIt = mapKeys.iterator()

            while (keyIt.hasNext()) {
                val key = keyIt.next()
                val comp1 = scores[key]

                if (comp1 == value) {
                    keyIt.remove()
                    sortedMap[key] = value
                    break
                }
            }
        }
        return sortedMap
    }
}

class Winner constructor(id: Long, score: Int, timestamp: Long) {
    var id: Long
    var score: Int
    var timestamp: Long

    init {
        this.id = id
        this.score = score
        this.timestamp = timestamp
    }
}