package com.timaf.chatfilter

import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import net.kyori.adventure.key.Key
import org.bukkit.NamespacedKey
import org.bukkit.Registry
import org.bukkit.Sound
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

// Reads and holds all settings from config.yml.
// Calling load() at any time reloads the config without restarting the server.
class FilterConfig(private val plugin: ChatFilterPlugin) {

    var bannedWords: List<String> = emptyList()
        private set

    var warningMessages: List<String> = emptyList()
        private set

    var sounds: List<Sound> = emptyList()
        private set

    var soundVolume: Float = 1.0f
        private set

    var soundPitch: Float = 1.0f
        private set

    var effects: List<PotionEffect> = emptyList()
        private set

    var kickEnabled: Boolean = false
        private set

    var kickThreshold: Int = 3
        private set

    var kickMessage: String = "You have been kicked."
        private set

    fun load() {
        plugin.reloadConfig()
        val config: FileConfiguration = plugin.config

        bannedWords = config.getStringList("banned-words")
            .map { it.lowercase().trim() }
            .filter { it.isNotEmpty() }

        warningMessages = config.getStringList("warning-messages")
            .filter { it.isNotBlank() }
            .ifEmpty { listOf("Watch your language!") }

        soundVolume = config.getDouble("sound.volume", 1.0).toFloat()
        soundPitch = config.getDouble("sound.pitch", 1.0).toFloat()

        // Paper 26.1+: Sound is no longer an enum but a registry-backed interface.
        // We resolve sounds by key via RegistryAccess instead of Sound.valueOf().
        val soundRegistry = RegistryAccess.registryAccess().getRegistry(RegistryKey.SOUND_EVENT)
        sounds = config.getStringList("sound.options")
            .mapNotNull { name ->
                // If no namespace is provided (e.g. "entity.villager.no"), default to "minecraft:".
                val keyString = if (name.contains(":")) name else "minecraft:$name"
                try {
                    val key = Key.key(keyString.lowercase())
                    val sound = soundRegistry.get(key)
                    if (sound == null) plugin.logger.warning("Invalid sound in config.yml: '$name' - not found in registry, skipping.")
                    sound
                } catch (e: IllegalArgumentException) {
                    plugin.logger.warning("Invalid sound key format in config.yml: '$name' - skipping.")
                    null
                }
            }

        effects = loadEffects(config)

        kickEnabled = config.getBoolean("kick.enabled", false)
        kickThreshold = config.getInt("kick.threshold", 3)
        kickMessage = config.getString("kick.message", "You have been kicked.") ?: "You have been kicked."
    }

    // Parses the "effects" list from config.yml into PotionEffect instances.
    // Each entry is expected in the format: { type: "blindness", duration-seconds: 1, amplifier: 0 }
    private fun loadEffects(config: FileConfiguration): List<PotionEffect> {
        return config.getMapList("effects").mapNotNull { entry ->
            val typeName = (entry["type"] as? String)?.trim()?.lowercase() ?: run {
                plugin.logger.warning("An effect entry in config.yml is missing the 'type' field, skipping.")
                return@mapNotNull null
            }

            // PotionEffectType is also registry-backed in modern Paper; access via Registry.EFFECT.
            val type: PotionEffectType? = Registry.EFFECT.get(NamespacedKey.minecraft(typeName))
            if (type == null) {
                plugin.logger.warning("Invalid effect type in config.yml: '$typeName' - not found in registry, skipping.")
                return@mapNotNull null
            }

            // Duration can be specified in seconds (preferred) or ticks (20 ticks = 1 second).
            val durationTicks = when {
                entry.containsKey("duration-seconds") ->
                    ((entry["duration-seconds"] as? Number)?.toDouble() ?: 1.0).times(20).toInt()
                entry.containsKey("duration-ticks") ->
                    (entry["duration-ticks"] as? Number)?.toInt() ?: 20
                else -> 20
            }

            val amplifier = (entry["amplifier"] as? Number)?.toInt() ?: 0

            // ambient=false: visible particles; particles=true: show effect particles; icon=true: show HUD icon.
            PotionEffect(type, durationTicks, amplifier, false, true, true)
        }
    }
}