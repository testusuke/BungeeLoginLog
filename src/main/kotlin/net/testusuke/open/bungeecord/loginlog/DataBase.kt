package net.testusuke.open.bungeecord.loginlog

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.TextComponent
import net.testusuke.open.bungeecord.loginlog.Main.Companion.plugin
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.SQLException

class DataBase(private val prefix: String) {

    //  Connect information
    private var host: String? = null
    private var user: String? = null
    private var pass: String? = null
    private var port: String? = null
    private var db: String? = null

    //  エラーモード
    private var errorMode = false

    init {
        //  Logger
        plugin.logger.info("DataBaseを読み込みます。")
        //  load config
        loadConfig()
        //  クラスローダー
        loadClass()
        //  Test Connect
        testConnect()
        //  createTable
        createTable()
        //  Logger
        plugin.logger.info("DataBaseを読み込みました。")
    }

    fun loadConfig() {
        host = plugin.config.getString("database.host")
        user = plugin.config.getString("database.user")
        pass = plugin.config.getString("database.pass")
        port = plugin.config.getString("database.port")
        db = plugin.config.getString("database.db")
    }

    private fun loadClass() {
        try {
            Class.forName("com.mysql.jdbc.Driver")
            plugin.logger.info("Load class.")
        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
            plugin.logger.info("DataBase connection class not found!")
        }
    }

    fun getConnection(): Connection? {
        val connection: Connection
        connection = try {
            DriverManager.getConnection("jdbc:mysql://$host:$port/$db", user, pass)
        } catch (e: SQLException) {
            e.printStackTrace()
            return null
        }
        return connection
    }

    private fun testConnect(): Boolean? {
        plugin.logger.info("接続テスト中....")
        if (getConnection() == null) {
            plugin.logger.info("接続に失敗しました。")
            return false
        }
        plugin.logger.info("接続に成功しました！")
        return true
    }

    private fun createTable(){
        val connection:Connection? = getConnection()
        if(connection == null){
            sendErrorMessage()
            return
        }
        val sql = "create table login_list\n" +
                "(\n" +
                "\t`index` int auto_increment,\n" +
                "\tname varchar(16) not null,\n" +
                "\tuuid varchar(36) not null,\n" +
                "\tip varchar(30) not null,\n" +
                "\tdate date not null,\n" +
                "\ttime time not null,\n" +
                "\tcountry varchar(20) null,\n" +
                "\tcity varchar(20) null,\n" +
                "\tconstraint login_list_pk\n" +
                "\t\tprimary key (`index`)\n" +
                ");\n"
        connection.createStatement().execute(sql)
        connection.close()
        plugin.logger.info("create table!")
    }

    @Synchronized
    fun savePlayerLog() {
        if (plugin.logDataList.isEmpty()) return
        val sql = "INSERT INTO login_log (name,uuid,ip,date,time,country,city) VALUES (?,?,?,?,?,?,?);"
        var connection: Connection?
        var statement: PreparedStatement?
        try {
            connection = getConnection()
            if (connection == null) {
                sendErrorMessage()
                return
            }
            connection.autoCommit = false
            statement = connection.prepareStatement(sql)
            for (logData in plugin.logDataList) {
                statement.setString(1, logData.name)
                statement.setString(2, logData.uuid)
                statement.setString(3, logData.ip)
                statement.setString(4, logData.date)
                statement.setString(5, logData.time)
                statement.setString(6,logData.country)
                statement.setString(7,logData.city)
                statement.addBatch()
                plugin.logger.info(statement.toString())
            }
            val result = statement.executeBatch()
            plugin.logger
                .info(ChatColor.BLUE.toString() + "ログインデータを" + ChatColor.YELLOW + result.size + "件" + ChatColor.BLUE + "登録しました。")
            try {
                connection.commit()
                plugin.logger.info("登録成功")
                plugin.logDataList.clear()
            } catch (e: SQLException) {
                connection.rollback()
                plugin.logger.info("ロールバックを実行します。")
                e.printStackTrace()
                sendErrorMessage()
                connection.close()
            }
            connection.close()
        } catch (e: SQLException) {
            e.printStackTrace()
            sendErrorMessage()
        }
    }
    private fun sendErrorMessage() {
        plugin.logger.warning(ChatColor.RED.toString() + "接続エラーです。DBがダウンしている、もしくはコネクションの設定を確認してください。")
        if (plugin.errorMode) return
        val textComponent =
            TextComponent(ChatColor.RED.toString() + "[BungeeLoginLog]データベース接続エラーです。運営に連絡してください。")
        for (serverInfo in plugin.proxy.servers.values) {
            for (player in serverInfo.players) {
                if (player.hasPermission("bungeecord.playerlog.admin")) {
                    player.sendMessage(textComponent)
                }
            }
        }
        plugin.errorMode = true
    }

}