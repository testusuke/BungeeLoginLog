package net.testusuke.open.bungeecord.loginlog

import com.google.common.io.ByteStreams
import net.md_5.bungee.api.plugin.Plugin
import net.md_5.bungee.config.Configuration
import net.md_5.bungee.config.ConfigurationProvider
import net.md_5.bungee.config.YamlConfiguration
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

class ConfigFile(plugin: Plugin) {
    private var plugin: Plugin? = null
    private var file: File? = null
    private var config: Configuration? = null

    init {
        this.plugin = plugin
        file = File(getDataFolder(), "config.yml")
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir()
        }
        if (!file!!.exists()) {
            try {
                file!!.createNewFile()
                getResourceAsStream("config.yml").use { `is` ->
                    FileOutputStream(file).use { os ->
                        ByteStreams.copy(
                            `is`,
                            os
                        )
                    }
                }
            } catch (e: IOException) {
                throw RuntimeException("Unable to create storage file", e)
            }
        }
    }

    fun getConfig(): Configuration? {
        return try {
            ConfigurationProvider.getProvider(YamlConfiguration::class.java).load(file)
                .also { config = it }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    fun saveConfig() {
        try {
            ConfigurationProvider.getProvider(YamlConfiguration::class.java)
                .save(config, File(getDataFolder(), "config.yml"))
        } catch (e: IOException) {
            e.printStackTrace()
            plugin!!.logger.severe("Couldn't save storage file!")
        }
    }

    private fun getDataFolder(): File {
        return plugin!!.dataFolder
    }

    private fun getResourceAsStream(file: String): InputStream {
        return plugin!!.getResourceAsStream(file)
    }

}