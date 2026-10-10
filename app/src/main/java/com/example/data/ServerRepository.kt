package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.ServerProtocol
import com.example.model.VpnServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.InetSocketAddress
import java.net.Socket

class ServerRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("dark_tunnel_servers_prefs", Context.MODE_PRIVATE)
    private val keyServers = "saved_servers_json"

    private val _servers = MutableStateFlow<List<VpnServer>>(emptyList())
    val servers: StateFlow<List<VpnServer>> = _servers.asStateFlow()

    private val _selectedServer = MutableStateFlow<VpnServer>(
        VpnServer(
            id = "server-sg-01",
            name = "Singapore Dark Tunnel Pro",
            countryName = "Singapore",
            countryCode = "SG",
            flagEmoji = "🇸🇬",
            host = "sg1.darktunnel.net",
            port = 443,
            supportedProtocols = listOf(ServerProtocol.SSH_WEBSOCKET, ServerProtocol.SSH_DIRECT, ServerProtocol.WEBSOCKET_TLS, ServerProtocol.DNS_TUNNEL),
            defaultProtocol = ServerProtocol.SSH_WEBSOCKET,
            pingMs = 28,
            serverLoadPercent = 22,
            isOnline = true,
            isPremium = false,
            city = "Singapore DC",
            wsPath = "/dark-ws",
            sniHost = "sg1.darktunnel.net",
            exitIp = "139.180.138.25"
        )
    )
    val selectedServer: StateFlow<VpnServer> = _selectedServer.asStateFlow()

    init {
        loadServers()
    }

    private fun loadServers() {
        val raw = prefs.getString(keyServers, null)
        val list = mutableListOf<VpnServer>()
        if (!raw.isNullOrBlank()) {
            try {
                val array = JSONArray(raw)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        VpnServer(
                            id = obj.optString("id", "srv-$i"),
                            name = obj.optString("name", "Dark Tunnel Server"),
                            countryName = obj.optString("countryName", "Singapore"),
                            countryCode = obj.optString("countryCode", "SG"),
                            flagEmoji = obj.optString("flagEmoji", "🇸🇬"),
                            host = obj.optString("host", "sg1.darktunnel.net"),
                            port = obj.optInt("port", 443),
                            supportedProtocols = listOf(ServerProtocol.SSH_WEBSOCKET, ServerProtocol.SSH_DIRECT, ServerProtocol.WEBSOCKET_TLS, ServerProtocol.DNS_TUNNEL),
                            defaultProtocol = ServerProtocol.SSH_WEBSOCKET,
                            pingMs = obj.optInt("pingMs", 30),
                            serverLoadPercent = obj.optInt("serverLoadPercent", 25),
                            isOnline = true,
                            isPremium = false,
                            city = obj.optString("city", "Datacenter"),
                            wsPath = obj.optString("wsPath", "/dark-ws"),
                            sniHost = obj.optString("sniHost", "sg1.darktunnel.net"),
                            exitIp = obj.optString("exitIp", "139.180.138.25")
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (list.isEmpty()) {
            // Seed default high-speed working servers identical to original Dark Tunnel VPN
            list.add(
                VpnServer(
                    id = "server-sg-01",
                    name = "Singapore Dark Tunnel Pro",
                    countryName = "Singapore",
                    countryCode = "SG",
                    flagEmoji = "🇸🇬",
                    host = "sg1.darktunnel.net",
                    port = 443,
                    supportedProtocols = listOf(ServerProtocol.SSH_WEBSOCKET, ServerProtocol.SSH_DIRECT, ServerProtocol.WEBSOCKET_TLS, ServerProtocol.DNS_TUNNEL),
                    defaultProtocol = ServerProtocol.SSH_WEBSOCKET,
                    pingMs = 28,
                    serverLoadPercent = 22,
                    isOnline = true,
                    isPremium = false,
                    city = "Singapore DC",
                    wsPath = "/dark-ws",
                    sniHost = "sg1.darktunnel.net",
                    exitIp = "139.180.138.25"
                )
            )
            list.add(
                VpnServer(
                    id = "server-us-01",
                    name = "United States Fast SSH",
                    countryName = "United States",
                    countryCode = "US",
                    flagEmoji = "🇺🇸",
                    host = "us1.darktunnel.net",
                    port = 443,
                    supportedProtocols = listOf(ServerProtocol.SSH_WEBSOCKET, ServerProtocol.SSH_DIRECT, ServerProtocol.WEBSOCKET_TLS, ServerProtocol.DNS_TUNNEL),
                    defaultProtocol = ServerProtocol.SSH_WEBSOCKET,
                    pingMs = 54,
                    serverLoadPercent = 35,
                    isOnline = true,
                    isPremium = false,
                    city = "New York DC",
                    wsPath = "/dark-ws",
                    sniHost = "us1.darktunnel.net",
                    exitIp = "192.241.140.88"
                )
            )
            list.add(
                VpnServer(
                    id = "server-de-01",
                    name = "Germany Premium Tunnel",
                    countryName = "Germany",
                    countryCode = "DE",
                    flagEmoji = "🇩🇪",
                    host = "de1.darktunnel.net",
                    port = 80,
                    supportedProtocols = listOf(ServerProtocol.SSH_WEBSOCKET, ServerProtocol.SSH_DIRECT, ServerProtocol.WEBSOCKET_TLS, ServerProtocol.DNS_TUNNEL),
                    defaultProtocol = ServerProtocol.SSH_WEBSOCKET,
                    pingMs = 72,
                    serverLoadPercent = 18,
                    isOnline = true,
                    isPremium = false,
                    city = "Frankfurt DC",
                    wsPath = "/dark-ws",
                    sniHost = "de1.darktunnel.net",
                    exitIp = "159.65.120.10"
                )
            )
            list.add(
                VpnServer(
                    id = "server-nl-01",
                    name = "Netherlands Secure WebSocket",
                    countryName = "Netherlands",
                    countryCode = "NL",
                    flagEmoji = "🇳🇱",
                    host = "nl1.darktunnel.net",
                    port = 443,
                    supportedProtocols = listOf(ServerProtocol.SSH_WEBSOCKET, ServerProtocol.SSH_DIRECT, ServerProtocol.WEBSOCKET_TLS, ServerProtocol.DNS_TUNNEL),
                    defaultProtocol = ServerProtocol.SSH_WEBSOCKET,
                    pingMs = 65,
                    serverLoadPercent = 28,
                    isOnline = true,
                    isPremium = false,
                    city = "Amsterdam DC",
                    wsPath = "/dark-ws",
                    sniHost = "nl1.darktunnel.net",
                    exitIp = "178.62.204.15"
                )
            )
            list.add(
                VpnServer(
                    id = "server-bd-01",
                    name = "Bangladesh Direct SSH",
                    countryName = "Bangladesh",
                    countryCode = "BD",
                    flagEmoji = "🇧🇩",
                    host = "104.16.132.229",
                    port = 80,
                    supportedProtocols = listOf(ServerProtocol.SSH_WEBSOCKET, ServerProtocol.SSH_DIRECT, ServerProtocol.WEBSOCKET_TLS, ServerProtocol.DNS_TUNNEL),
                    defaultProtocol = ServerProtocol.SSH_DIRECT,
                    pingMs = 18,
                    serverLoadPercent = 15,
                    isOnline = true,
                    isPremium = false,
                    city = "Dhaka Local",
                    wsPath = "/dark-ws",
                    sniHost = "cloudflare.com",
                    exitIp = "104.16.132.229"
                )
            )
            saveListToPrefs(list)
        }

        _servers.value = list
        _selectedServer.value = list.first()
    }

    private fun saveListToPrefs(list: List<VpnServer>) {
        val array = JSONArray()
        for (server in list) {
            val obj = JSONObject().apply {
                put("id", server.id)
                put("name", server.name)
                put("countryName", server.countryName)
                put("countryCode", server.countryCode)
                put("flagEmoji", server.flagEmoji)
                put("host", server.host)
                put("port", server.port)
                put("pingMs", server.pingMs)
                put("serverLoadPercent", server.serverLoadPercent)
                put("city", server.city)
                put("wsPath", server.wsPath)
                put("sniHost", server.sniHost)
                put("exitIp", server.exitIp)
            }
            array.put(obj)
        }
        prefs.edit().putString(keyServers, array.toString()).apply()
    }

    fun selectServer(server: VpnServer) {
        _selectedServer.value = server
    }

    fun saveServer(server: VpnServer) {
        val current = _servers.value.toMutableList()
        val index = current.indexOfFirst { it.id == server.id }
        if (index >= 0) {
            current[index] = server
        } else {
            current.add(0, server)
        }
        saveListToPrefs(current)
        _servers.value = current
        if (_selectedServer.value.id == server.id) {
            _selectedServer.value = server
        }
    }

    fun deleteServer(serverId: String) {
        val current = _servers.value.toMutableList()
        current.removeAll { it.id == serverId }
        if (current.isEmpty()) {
            val fallback = VpnServer(
                id = "server-custom-01",
                name = "My Custom Server",
                countryName = "Custom Setup",
                countryCode = "XX",
                flagEmoji = "⚡",
                host = "sg1.darktunnel.net",
                port = 443,
                supportedProtocols = listOf(ServerProtocol.SSH_WEBSOCKET, ServerProtocol.SSH_DIRECT, ServerProtocol.WEBSOCKET_TLS, ServerProtocol.DNS_TUNNEL),
                defaultProtocol = ServerProtocol.SSH_WEBSOCKET,
                pingMs = 28,
                serverLoadPercent = 10,
                isOnline = true,
                isPremium = false,
                city = "Custom Setup",
                wsPath = "/dark-ws",
                sniHost = "sg1.darktunnel.net",
                exitIp = "139.180.138.25"
            )
            current.add(fallback)
        }
        saveListToPrefs(current)
        _servers.value = current
        if (_selectedServer.value.id == serverId) {
            _selectedServer.value = current.first()
        }
    }

    suspend fun refreshAllPings() = withContext(Dispatchers.IO) {
        val current = _servers.value.map { server ->
            if (server.host.isBlank()) {
                server.copy(pingMs = 0, isOnline = true)
            } else {
                val latency = measurePing(server.host, server.port)
                server.copy(pingMs = latency, isOnline = latency in 1..2000)
            }
        }
        _servers.value = current
        if (current.any { it.id == _selectedServer.value.id }) {
            _selectedServer.value = current.first { it.id == _selectedServer.value.id }
        }
    }

    private suspend fun measurePing(host: String, port: Int): Int = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        return@withContext try {
            val socket = Socket()
            socket.connect(InetSocketAddress(host, port), 2500)
            socket.close()
            val duration = (System.currentTimeMillis() - start).toInt()
            duration.coerceIn(5, 999)
        } catch (_: Exception) {
            -1
        }
    }
}
