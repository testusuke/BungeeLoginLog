package net.testusuke.open.bungeecord.loginlog

import com.google.common.io.ByteStreams
import com.maxmind.geoip2.DatabaseReader
import net.md_5.bungee.api.event.PostLoginEvent
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.api.plugin.Plugin
import net.md_5.bungee.config.Configuration
import net.md_5.bungee.event.EventHandler
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.InetAddress
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class Main: Plugin(),Listener {

    companion object{
        lateinit var plugin:Main

    }
    var errorMode = false
    var logDataList: MutableList<LogData> = mutableListOf()
    //  config
    lateinit var configFile: ConfigFile
    lateinit var config: Configuration
    //db
    lateinit var dataBase: DataBase
    //  GeoIP
    lateinit var databaseFile: File
    //  name
    private val mmdb = "GeoLite2-City.mmdb"


    override fun onEnable() {
        plugin = this
        //  Logger

        //  Config
        configFile = ConfigFile(this)
        config = configFile.config
        //  DataBase
        dataBase = DataBase("BungeeLoginLog")
        //  Load DataBase File
        databaseFile = File(dataFolder, mmdb)

        if (!dataFolder.exists()) {
            dataFolder.mkdir()
        }
        if (!databaseFile.exists()) {
            try {
                databaseFile.createNewFile()
                getResourceAsStream(mmdb).use { `is` ->
                    FileOutputStream(
                        databaseFile
                    ).use({ os -> ByteStreams.copy(`is`, os) })
                }
            } catch (e: IOException) {
                throw RuntimeException("Unable to create storage file", e)
            }
        }

        //  Run
        proxy.scheduler.schedule(this, Runnable() {
            fun run(){
                logger.info("start saving...")
                dataBase.savePlayerLog()
            }
        },20,60*2, TimeUnit.SECONDS)

    }

    override fun onDisable() {
        dataBase.savePlayerLog()
    }

    //  Login
    @EventHandler
    fun onLogin(event: PostLoginEvent) {

        proxy.scheduler.schedule(this, Runnable() {
            fun run (){
                val player = event.player
                val name = player.name
                val uuid = player.uniqueId.toString()
                val ip = player.socketAddress.toString()
                val calendar = Calendar.getInstance()
                val dateSDF = SimpleDateFormat("yyyy-MM-dd")
                val date = dateSDF.format(calendar.time)
                val timeSDF = SimpleDateFormat("HH:mm:ss")
                val time = timeSDF.format(calendar.time)
                //  DB
                var country = ""
                var city = ""
                try {
                    val dbr = DatabaseReader.Builder(databaseFile).build()
                    val address = InetAddress.getByName(ip)
                    val cityResponse = dbr.city(address)
                    country = cityResponse.country.name ?: "none"
                    city = cityResponse.city.name ?: "none"
                }catch (e:Exception){
                    addLogData(name,uuid,ip,date,time,"none","none")
                }
                //  Add
                addLogData(name, uuid, ip, date, time,country,city)
            }
        },1,TimeUnit.SECONDS)
    }

    private fun addLogData(
        name: String,
        uuid: String,
        ip: String,
        date: String,
        time: String,
        country:String,
        city:String
    ) {
        val logData = LogData(name, uuid, ip, date, time,country,city)
        logDataList.add(logData)
    }
}