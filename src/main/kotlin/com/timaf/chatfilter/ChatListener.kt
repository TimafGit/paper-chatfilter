package com.timaf.chatfilter

import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.title.Title
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.potion.PotionEffect
import java.time.Duration
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

class ChatListener(private val plugin: ChatFilterPlugin) : Listener {

    // PlainTextComponentSerializer is used to extract raw text from the Adventure Component
    // that Paper provides for chat messages instead of a plain String.
    private val plainSerializer = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()

    // Title timing for a jumpscare effect:
    // near-instant fade-in, short stay, quick fade-out.
    private val titleTimes = Title.Times.times(
        Duration.ofMillis(50),
        Duration.ofMillis(900),
        Duration.ofMillis(200)
    )

    // Tracks how many violations each player has accumulated.
    // ConcurrentHashMap is used because AsyncChatEvent runs off the main thread,
    // and multiple players could trigger this simultaneously.
    private val violationCount = ConcurrentHashMap<UUID, Int>()

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onChat(event: AsyncChatEvent) {
        val rawMessage = plainSerializer.serialize(event.message())
        val normalized = normalize(rawMessage)

        val config = plugin.filterConfig
        val matchedWord = config.bannedWords.firstOrNull { normalized.contains(it) }

        if (matchedWord != null) {
            event.isCancelled = true

            val player = event.player
            val uuid = player.uniqueId

            // Increment violation counter atomically to avoid race conditions.
            val count = violationCount.merge(uuid, 1, Int::plus) ?: 1

            plugin.logger.info("[ChatFilter] ${player.name} used a banned word (match: '$matchedWord'), total violations: $count")

            if (config.kickEnabled && count >= config.kickThreshold) {
                violationCount.remove(uuid)
                // Kick must be executed on the main thread.
                plugin.server.scheduler.runTask(plugin, Runnable {
                    player.kick(Component.text(config.kickMessage, NamedTextColor.RED))
                })
            } else {
                // All Bukkit API calls must run on the main thread,
                // since this event handler is called asynchronously.
                plugin.server.scheduler.runTask(plugin, Runnable {
                    playWarningSound(player)
                    showWarningTitle(player)
                    applyEffects(player)
                })
            }
        }
    }

    // Clean up the violation counter when a player leaves
    // to avoid unnecessary memory usage.
    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        violationCount.remove(event.player.uniqueId)
    }

    // Strips everything except letters and digits to catch
    // simple bypass attempts like "a.m.k" or "a m k".
    // Note: this is not bypass-proof, it only provides basic protection.
    private fun normalize(input: String): String {
        return input.lowercase()
            .replace(Regex("[^a-zçğıöşü0-9]"), "")
    }

    private fun playWarningSound(player: Player) {
        val sounds = plugin.filterConfig.sounds
        if (sounds.isEmpty()) return
        val sound = sounds[Random.nextInt(sounds.size)]
        player.playSound(player.location, sound, plugin.filterConfig.soundVolume, plugin.filterConfig.soundPitch)
    }

    private fun showWarningTitle(player: Player) {
        val messages = plugin.filterConfig.warningMessages
        if (messages.isEmpty()) return
        val text = messages[Random.nextInt(messages.size)]
        val mainTitle = Component.text(text, NamedTextColor.RED)
            .decorate(TextDecoration.BOLD)
        val title = Title.title(mainTitle, Component.empty(), titleTimes)
        player.showTitle(title)
    }

    // Applies all potion effects defined in the "effects" section of config.yml.
    private fun applyEffects(player: Player) {
        for (effect: PotionEffect in plugin.filterConfig.effects) {
            player.addPotionEffect(effect)
        }
    }
}