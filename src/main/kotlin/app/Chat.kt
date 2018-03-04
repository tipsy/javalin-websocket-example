package app

import io.javalin.Javalin
import io.javalin.embeddedserver.jetty.websocket.WsSession
import j2html.TagCreator.*
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap

private val userUsernameMap = ConcurrentHashMap<WsSession, String>()
private var nextUserNumber = 1 // Assign to username for next connecting user

fun main(args: Array<String>) {
    Javalin.create().apply {
        port(7070)
        enableStaticFiles("/public")
        ws("/chat") { ws ->
            ws.onConnect { session ->
                val username = "User" + nextUserNumber++
                userUsernameMap.put(session, username)
                broadcastMessage("Server", username + " joined the chat")
            }
            ws.onClose { session, status, message ->
                val username = userUsernameMap[session]
                userUsernameMap.remove(session)
                broadcastMessage("Server", username + " left the chat")
            }
            ws.onMessage { session, message ->
                broadcastMessage(userUsernameMap[session]!!, message)
            }
        }
    }.start()
}

// Sends a message from one user to all users, along with a list of current usernames
fun broadcastMessage(sender: String, message: String) {
    userUsernameMap.keys.filter { it.isOpen }.forEach { session ->
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
            b(sender + " says:"),
            span(attrs(".timestamp"), SimpleDateFormat("HH:mm:ss").format(Date())),
            p(message)
    ).render()
}

