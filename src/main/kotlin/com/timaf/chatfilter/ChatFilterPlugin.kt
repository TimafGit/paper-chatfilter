package com.timaf.chatfilter

import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.plugin.java.JavaPlugin

class ChatFilterPlugin : JavaPlugin() {

    lateinit var filterConfig: FilterConfig
        private set

    override fun onEnable() {
        saveDefaultConfig()
        filterConfig = FilterConfig(this)
        filterConfig.load()

        server.pluginManager.registerEvents(ChatListener(this), this)

        logger.info("ChatFilter enabled. ${filterConfig.bannedWords.size} banned words, ${filterConfig.effects.size} effects loaded.")
    }

    override fun onDisable() {
        logger.info("ChatFilter disabled.")
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (command.name != "chatfilter") return false

        if (args.isEmpty() || args[0].lowercase() != "reload") {
            sender.sendMessage("§eUsage: /chatfilter reload")
            return true
        }

        if (!sender.hasPermission("chatfilter.admin")) {
            sender.sendMessage("§cYou don't have permission to use this command.")
            return true
        }

        filterConfig.load()
        sender.sendMessage("§aChatFilter config reloaded! ${filterConfig.bannedWords.size} words, ${filterConfig.effects.size} effects active.")
        logger.info("Config reloaded by ${sender.name}.")
        return true
    }
}