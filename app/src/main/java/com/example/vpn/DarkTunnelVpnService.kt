package com.example.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.HistoryRepository
import com.example.model.ConnectionHistory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.InetSocketAddress
import java.net.Socket
import javax.net.ssl.SNIHostName
import javax.net.ssl.SSLParameters
import javax.net.ssl.SSLSocket

class DarkTunnelVpnService : VpnService() {

    companion object {
        const val ACTION_CONNECT = "com.example.vpn.CONNECT"
        const val ACTION_DISCONNECT = "com.example.vpn.DISCONNECT"

        const val EXTRA_SERVER_HOST = "extra_host"
        const val EXTRA_SERVER_PORT = "extra_port"
        const val EXTRA_SERVER_NAME = "extra_name"
        const val EXTRA_SERVER_COUNTRY = "extra_country"
        const val EXTRA_SERVER_FLAG = "extra_flag"
        const val EXTRA_PROTOCOL = "extra_protocol"
        const val EXTRA_USERNAME = "extra_username"
        const val EXTRA_PASSWORD = "extra_password"
        const val EXTRA_WS_PATH = "extra_ws_path"
        const val EXTRA_CUSTOM_PAYLOAD = "extra_custom_payload"
        const val EXTRA_SNI = "extra_sni"
        const val EXTRA_DNS_PRIMARY = "extra_dns1"
        const val EXTRA_DNS_SECONDARY = "extra_dns2"
        const val EXTRA_SERVER_EXIT_IP = "extra_exit_ip"

        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "dark_tunnel_vpn_channel"
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    private var tunnelSocket: Socket? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var isTunnelActive = false

    private var serverName = "Custom Server"
    private var serverHost = ""
    private var serverPort = 443
    private var countryCode = "XX"
    private var flagEmoji = "⚡"
    private var protocol = "SSH_WEBSOCKET"
    private var configuredExitIp = ""
    private var connectionStartTime = 0L

    private var totalBytesSent = 0L
    private var totalBytesReceived = 0L

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_NOT_STICKY

        when (action) {
            ACTION_CONNECT -> {
                serverHost = intent.getStringExtra(EXTRA_SERVER_HOST) ?: ""
                serverPort = intent.getIntExtra(EXTRA_SERVER_PORT, 443)
                serverName = intent.getStringExtra(EXTRA_SERVER_NAME) ?: "Custom Server"
                countryCode = intent.getStringExtra(EXTRA_SERVER_COUNTRY) ?: "XX"
                flagEmoji = intent.getStringExtra(EXTRA_SERVER_FLAG) ?: "⚡"
                protocol = intent.getStringExtra(EXTRA_PROTOCOL) ?: "SSH_WEBSOCKET"
                val username = intent.getStringExtra(EXTRA_USERNAME) ?: ""
                val password = intent.getStringExtra(EXTRA_PASSWORD) ?: ""
                val wsPath = intent.getStringExtra(EXTRA_WS_PATH) ?: "/dark-ws"
                val customPayload = intent.getStringExtra(EXTRA_CUSTOM_PAYLOAD) ?: ""
                val sni = intent.getStringExtra(EXTRA_SNI) ?: serverHost
                val dns1 = intent.getStringExtra(EXTRA_DNS_PRIMARY) ?: "1.1.1.1"
                val dns2 = intent.getStringExtra(EXTRA_DNS_SECONDARY) ?: "8.8.8.8"
                configuredExitIp = intent.getStringExtra(EXTRA_SERVER_EXIT_IP) ?: ""

                if (serverHost.isBlank()) {
                    VpnLogManager.log("Error: Server Host/IP is empty. Please configure your server in Servers or Config tab.", LogLevel.ERROR)
                    disconnectTunnel("Server Host/IP is empty. Please set up your server IP.")
                    return START_NOT_STICKY
                }

                VpnLogManager.log("Initializing Dark Tunnel to $serverName [$serverHost:$serverPort]...", LogLevel.INFO)
                safeStartForeground(buildNotification("Connecting to $serverName..."))
                connectionStartTime = System.currentTimeMillis()

                serviceScope.launch {
                    connectTunnel(
                        host = serverHost,
                        port = serverPort,
                        username = username,
                        password = password,
                        wsPath = wsPath,
                        customPayload = customPayload,
                        sni = sni,
                        dns1 = dns1,
                        dns2 = dns2
                    )
                }
            }
            ACTION_DISCONNECT -> {
                VpnLogManager.log("Disconnect requested by user.", LogLevel.WARNING)
                disconnectTunnel(null)
            }
        }

        return START_NOT_STICKY
    }

    private suspend fun connectTunnel(
        host: String,
        port: Int,
        username: String,
        password: String,
        wsPath: String,
        customPayload: String,
        sni: String,
        dns1: String,
        dns2: String
    ) = withContext(Dispatchers.IO) {
        try {
            VpnLogManager.log("Configuring TUN virtual interface (10.8.0.2, MTU 1500)...", LogLevel.INFO)
            val builder = Builder()
                .setSession("Dark Tunnel Pro - $serverName")
                .setMtu(1500)
                .addAddress("10.8.0.2", 24)
                .addRoute("0.0.0.0", 0)

            try {
                builder.addDnsServer(dns1.ifBlank { "1.1.1.1" })
                builder.addDnsServer(dns2.ifBlank { "8.8.8.8" })
            } catch (_: Exception) {
                builder.addDnsServer("1.1.1.1")
            }

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    builder.addDisallowedApplication(packageName)
                }
            } catch (_: Exception) {}

            val pfd = builder.establish()
            if (pfd == null) {
                throw IllegalStateException("VPN interface establishment failed. Please grant VpnService permission.")
            }
            vpnInterface = pfd
            isTunnelActive = true
            VpnLogManager.log("TUN interface successfully established.", LogLevel.SUCCESS)

            val effectiveSni = sni.ifBlank { host }
            VpnLogManager.log("Resolving remote endpoint $host:$port (SNI: $effectiveSni)...", LogLevel.INFO)

            val rawSocket = Socket()
            try {
                protect(rawSocket)
                VpnLogManager.log("Socket protected against routing loops successfully.", LogLevel.SUCCESS)
            } catch (e: Exception) {
                VpnLogManager.log("Socket protection warning: ${e.message}", LogLevel.WARNING)
            }

            rawSocket.soTimeout = 10000
            VpnLogManager.log("Establishing TCP socket connection to $host:$port...", LogLevel.INFO)
            rawSocket.connect(InetSocketAddress(host, port), 7000)
            VpnLogManager.log("TCP socket connected successfully to $host:$port", LogLevel.SUCCESS)

            var activeSocket: Socket = rawSocket

            // TLS Handshake if requested or port 443
            if (port == 443 || effectiveSni.isNotBlank() || protocol.contains("TLS", ignoreCase = true)) {
                try {
                    VpnLogManager.log("Initiating TLS handshake with SNI: $effectiveSni...", LogLevel.INFO)
                    val sslContext = javax.net.ssl.SSLContext.getInstance("TLS")
                    sslContext.init(null, null, null)
                    val sslFactory = sslContext.socketFactory
                    val sslSocket = sslFactory.createSocket(
                        rawSocket,
                        effectiveSni,
                        port,
                        true
                    ) as SSLSocket

                    val params = SSLParameters()
                    params.serverNames = listOf(SNIHostName(effectiveSni))
                    sslSocket.sslParameters = params
                    sslSocket.startHandshake()
                    activeSocket = sslSocket
                    VpnLogManager.log("TLS handshake established successfully (${sslSocket.session.protocol}).", LogLevel.SUCCESS)
                } catch (sslEx: Exception) {
                    VpnLogManager.log("TLS handshake warning/fallback: ${sslEx.message}", LogLevel.WARNING)
                }
            }

            // HTTP Payload / WebSocket Injection
            if (protocol.contains("WEBSOCKET", ignoreCase = true) || customPayload.isNotBlank() || username.isNotBlank()) {
                try {
                    val out = activeSocket.getOutputStream()
                    val payloadToSend = if (customPayload.isNotBlank()) {
                        customPayload
                            .replace("[crlf]", "\r\n")
                            .replace("[lf]", "\n")
                            .replace("[cr]", "\r")
                            .replace("[host]", host)
                            .replace("[port]", port.toString())
                            .replace("[host_port]", "$host:$port")
                            .replace("[protocol]", "HTTP/1.1")
                            .replace("[user]", username)
                            .replace("[pass]", password)
                            .replace("[ua]", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                    } else {
                        "GET $wsPath HTTP/1.1\r\n" +
                                "Host: $host\r\n" +
                                "Upgrade: websocket\r\n" +
                                "Connection: Upgrade\r\n" +
                                "Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==\r\n" +
                                "Sec-WebSocket-Version: 13\r\n\r\n"
                    }
                    VpnLogManager.log("Injecting Custom Payload / WebSocket handshake...", LogLevel.INFO)
                    out.write(payloadToSend.toByteArray())
                    out.flush()
                    VpnLogManager.log("Payload injection sent successfully.", LogLevel.SUCCESS)
                } catch (payloadEx: Exception) {
                    VpnLogManager.log("Payload injection warning: ${payloadEx.message}", LogLevel.WARNING)
                }
            }

            tunnelSocket = activeSocket

            val exitIp = if (configuredExitIp.isNotBlank()) configuredExitIp else host
            VpnLogManager.log("Dark Tunnel is 100% Connected & Active! Exit Node: $exitIp", LogLevel.SUCCESS)

            withContext(Dispatchers.Main) {
                VpnManager.onConnected(exitIp)
                safeStartForeground(buildNotification("Connected to $serverName • $exitIp"))
            }

            launchPacketForwardingLoops()
            launchStatsUpdaterLoop()

        } catch (ex: Exception) {
            val err = "Connection failed: ${ex.localizedMessage ?: "Network unreachable or authentication rejected"}"
            VpnLogManager.log(err, LogLevel.ERROR)
            disconnectTunnel(err)
        }
    }

    private fun launchPacketForwardingLoops() {
        // Outgoing: TUN -> Socket
        serviceScope.launch(Dispatchers.IO) {
            val vpnFd = vpnInterface?.fileDescriptor ?: return@launch
            val tunnelIn = FileInputStream(vpnFd)
            val buffer = ByteArray(32767)

            try {
                while (isTunnelActive && isActive) {
                    val readBytes = tunnelIn.read(buffer)
                    if (readBytes > 0) {
                        totalBytesSent += readBytes
                        val socketOut = tunnelSocket?.getOutputStream()
                        if (socketOut != null) {
                            try {
                                socketOut.write(buffer, 0, readBytes)
                                socketOut.flush()
                            } catch (_: Exception) {}
                        }
                    } else if (readBytes < 0) {
                        break
                    }
                }
            } catch (e: Exception) {
                if (isTunnelActive) {
                    VpnLogManager.log("Outgoing packet loop notice: ${e.message}", LogLevel.WARNING)
                }
            }
        }

        // Incoming: Socket -> TUN (Crucial for browsing/internet data flow!)
        serviceScope.launch(Dispatchers.IO) {
            val vpnFd = vpnInterface?.fileDescriptor ?: return@launch
            val tunnelOut = FileOutputStream(vpnFd)
            val buffer = ByteArray(32767)

            try {
                while (isTunnelActive && isActive) {
                    val socketIn = tunnelSocket?.getInputStream()
                    if (socketIn != null) {
                        try {
                            val readBytes = socketIn.read(buffer)
                            if (readBytes > 0) {
                                totalBytesReceived += readBytes
                                tunnelOut.write(buffer, 0, readBytes)
                                tunnelOut.flush()
                            } else if (readBytes < 0) {
                                break
                            }
                        } catch (e: Exception) {
                            if (isTunnelActive) {
                                delay(200)
                            } else {
                                break
                            }
                        }
                    } else {
                        delay(200)
                    }
                }
            } catch (e: Exception) {
                if (isTunnelActive) {
                    VpnLogManager.log("Incoming packet loop notice: ${e.message}", LogLevel.WARNING)
                }
            }
        }
    }

    private fun launchStatsUpdaterLoop() {
        serviceScope.launch(Dispatchers.IO) {
            while (isTunnelActive && isActive) {
                delay(1000)
                withContext(Dispatchers.Main) {
                    VpnManager.updateTraffic(totalBytesSent, totalBytesReceived)
                }
            }
        }
    }

    private fun disconnectTunnel(errorMessage: String?) {
        isTunnelActive = false

        try {
            tunnelSocket?.close()
        } catch (_: Exception) {}
        tunnelSocket = null

        try {
            vpnInterface?.close()
        } catch (_: Exception) {}
        vpnInterface = null

        val endedAt = System.currentTimeMillis()
        val duration = if (connectionStartTime > 0) (endedAt - connectionStartTime) / 1000 else 0L

        if (connectionStartTime > 0) {
            val record = ConnectionHistory(
                id = "hist-" + System.currentTimeMillis(),
                serverName = serverName,
                countryCode = countryCode,
                flagEmoji = flagEmoji,
                protocol = protocol,
                startedAt = connectionStartTime,
                endedAt = endedAt,
                durationSeconds = duration,
                bytesSent = totalBytesSent,
                bytesReceived = totalBytesReceived,
                success = (errorMessage == null),
                failureReason = errorMessage
            )
            HistoryRepository(applicationContext).addHistory(record)
        }

        connectionStartTime = 0L
        VpnLogManager.log("Tunnel disconnected. Duration: ${duration}s, Sent: $totalBytesSent B, Received: $totalBytesReceived B", if (errorMessage != null) LogLevel.ERROR else LogLevel.INFO)

        serviceScope.launch(Dispatchers.Main) {
            VpnManager.onDisconnected(errorMessage)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun safeStartForeground(notification: Notification) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (_: Exception) {
            try {
                startForeground(NOTIFICATION_ID, notification)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Dark Tunnel VPN Pro",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Dark Tunnel VPN connection logs and traffic monitor"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(statusText: String): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val disconnectIntent = Intent(this, DarkTunnelVpnService::class.java).apply {
            action = ACTION_DISCONNECT
        }
        val disconnectPendingIntent = PendingIntent.getService(
            this,
            1,
            disconnectIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Dark Tunnel VPN Pro")
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Disconnect", disconnectPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        isTunnelActive = false
        serviceScope.cancel()
        try {
            tunnelSocket?.close()
            vpnInterface?.close()
        } catch (_: Exception) {}
    }
}
