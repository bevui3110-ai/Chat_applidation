package server.manager;

import server.core.ClientSession;
import java.util.concurrent.ConcurrentHashMap;

public class RoomManager {
    private static ConcurrentHashMap<String, ClientSession> onlineClients = new ConcurrentHashMap<>();

    public static void addClient(String username, ClientSession session) {
        onlineClients.put(username, session);
    }

    public static void removeClient(String username) {
        if (username != null) {
            onlineClients.remove(username);
        }
    }

    public static ClientSession getClient(String username) {
        return onlineClients.get(username);
    }

    public static ConcurrentHashMap<String, ClientSession> getOnlineClients() {
        return onlineClients;
    }
}