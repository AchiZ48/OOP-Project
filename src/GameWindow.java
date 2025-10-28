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
    private boolean fullscreen = false;
    private Rectangle windowedBounds;
    private final GraphicsDevice graphicsDevice;

    public GameWindow() {
        GraphicsEnvironment environment = GraphicsEnvironment.getLocalGraphicsEnvironment();
        graphicsDevice = environment.getDefaultScreenDevice();
        frame = new JFrame("Solstice Warriors DEMO");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        panel = new GamePanel(virtualWidth, virtualHeight);
        panel.setFullscreenToggleHandler(this::toggleFullscreen);
        frame.setContentPane(panel);
        frame.setSize(960, 640);
        frame.setLocationRelativeTo(null);
        frame.setMinimumSize(new Dimension(400, 300));
        frame.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                panel.onWindowResize();
            }
        });
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

    private void setFullscreen(boolean enable) {
        if (fullscreen == enable) {
            return;
        }

        if (enable) {
            windowedBounds = frame.getBounds();
            frame.dispose();
            frame.setUndecorated(true);
            frame.setResizable(false);
            frame.setContentPane(panel);
            frame.setVisible(true);
            if (graphicsDevice != null && graphicsDevice.isFullScreenSupported()) {
                graphicsDevice.setFullScreenWindow(frame);
            } else {
                frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            }
        } else {
            if (graphicsDevice != null && graphicsDevice.getFullScreenWindow() == frame) {
                graphicsDevice.setFullScreenWindow(null);
            }
            frame.dispose();
            frame.setUndecorated(false);
            frame.setResizable(true);
            frame.setContentPane(panel);
            if (windowedBounds != null) {
                frame.setBounds(windowedBounds);
            } else {
                frame.setSize(960, 640);
                frame.setLocationRelativeTo(null);
            }
            frame.setExtendedState(JFrame.NORMAL);
            frame.setVisible(true);
        }

        fullscreen = enable;
        frame.revalidate();
        frame.repaint();
        panel.onWindowResize();
        SwingUtilities.invokeLater(panel::requestFocusInWindow);
    }

    private void toggleFullscreen() {
        if (SwingUtilities.isEventDispatchThread()) {
            setFullscreen(!fullscreen);
        } else {
            SwingUtilities.invokeLater(() -> setFullscreen(!fullscreen));
        }
    }
}
