import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jetty.websocket.api.Session;
import org.json.JSONObject;

import io.javalin.Javalin;
import util.HerokuUtil;

import static j2html.TagCreator.*;

public class Chat {

    private static Map<Session, String> userUsernameMap = new ConcurrentHashMap<>();
    private static int nextUserNumber = 1; // Assign to username for next connecting user

    public static void main(String[] args) {
        Javalin.create()
            .port(HerokuUtil.getHerokuAssignedPort())
            .enableStaticFiles("/public")
            .ws("/chat", ws -> {
                ws.onConnect(session -> {
                    String username = "User" + nextUserNumber++;
                    userUsernameMap.put(session, username);
                    broadcastMessage("Server", (username + " joined the chat"));
                });
                ws.onClose((session, status, message) -> {
                    String username = userUsernameMap.get(session);
                    userUsernameMap.remove(session);
                    broadcastMessage("Server", (username + " left the chat"));
                });
                ws.onMessage((session, message) -> {
                    broadcastMessage(userUsernameMap.get(session), message);
                });
            })
            .start();
    }

    // Sends a message from one user to all users, along with a list of current usernames
    private static void broadcastMessage(String sender, String message) {
        userUsernameMap.keySet().stream().filter(Session::isOpen).forEach(session -> {
            try {
                session.getRemote().sendString(
                    new JSONObject()
                        .put("userMessage", createHtmlMessageFromSender(sender, message))
                        .put("userlist", userUsernameMap.values()).toString()
                );
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    // Builds a HTML element with a sender-name, a message, and a timestamp
    private static String createHtmlMessageFromSender(String sender, String message) {
        return article(
            b(sender + " says:"),
            span(attrs(".timestamp"), new SimpleDateFormat("HH:mm:ss").format(new Date())),
            p(message)
        ).render();
    }

}
