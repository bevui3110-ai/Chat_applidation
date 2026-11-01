package server;

import server.gui.ServerDashboardFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class ServerMain {

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            ServerDashboardFrame dashboard = new ServerDashboardFrame();
            dashboard.setVisible(true);
        });
    }
}
