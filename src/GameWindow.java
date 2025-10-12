import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class GameWindow {
    final int virtualWidth = 640;
    final int virtualHeight = 360;
    JFrame frame;
    GamePanel panel;

    public GameWindow() {
        frame = new JFrame("Solstice Warriors DEMO");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        panel = new GamePanel(virtualWidth, virtualHeight);
        frame.setContentPane(panel);
        frame.setSize(960, 640);
        frame.setLocationRelativeTo(null);
        frame.setMinimumSize(new Dimension(400, 300));
        frame.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                panel.onWindowResize();
            }
        });

        // Ensure clean shutdown
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                panel.stop();
                System.exit(0);
            }
        });
    }

    public void show() {
        frame.setVisible(true);
        panel.start();
    }
}
