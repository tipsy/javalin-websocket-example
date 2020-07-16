package app

import io.javalin.Javalin
import io.javalin.websocket.WsContext
import j2html.TagCreator.*
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap

private val userUsernameMap = ConcurrentHashMap<WsContext, String>()
private val nextUserNumber = java.util.concurrent.atomic.AtomicInteger(1) // Assign to username for next connecting user
private val servers = mutableMapOf<Int, Javalin>()
val NServers = 10
val BasePort= 7090
fun startServer(port: Int): Javalin {
    println("Starting server on port $port ..")
    val server = Javalin.create {
        it.addStaticFiles("/public")
    }.apply {
        ws("/chat") { ws ->
            ws.onConnect { ctx ->
                val username = "User" + nextUserNumber.getAndIncrement()
                userUsernameMap.put(ctx, username)
                broadcastMessage("Server", "$username joined the chat")
            }
            ws.onClose { ctx ->
                val username = userUsernameMap[ctx]
                userUsernameMap.remove(ctx)
                broadcastMessage("Server", "$username left the chat")
            }
            ws.onMessage { ctx ->
                broadcastMessage(userUsernameMap[ctx]!!, ctx.message())
            }
        }
    }.start(port)
    return server
}

fun main(args: Array<String>) {
    (1 until NServers).forEach { n ->
        val port = BasePort + n
        servers[port] = startServer(port)
    }
    Thread.currentThread().join()
}

// Sends a message from one user to all users, along with a list of current usernames
fun broadcastMessage(sender: String, message: String) {
    userUsernameMap.keys.filter { it.session.isOpen }.forEach { session ->
        session.send(
                JSONObject()
                        .put("userMessage", createHtmlMessageFromSender(sender, message))
                        .put("userlist", userUsernameMap.values).toString()
        )
    }
}

// Builds a HTML element with a sender-name, a message, and a timestamp,
private fun createHtmlMessageFromSender(sender: String, message: String): String {
    return article(
            b("$sender says:"),
            span(attrs(".timestamp"), SimpleDateFormat("HH:mm:ss").format(Date())),
            p(message)
    ).render()
}

