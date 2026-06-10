import javax.swing.*;
import java.awt.*;

public class WindowTest {
    public static void main(String[] args) throws Exception {
        System.out.println("OS: " + System.getProperty("os.name") + " " + System.getProperty("os.version"));
        System.out.println("Java: " + System.getProperty("java.version"));
        System.out.println("Arch: " + System.getProperty("os.arch"));
        System.out.println("GraphicsEnv: " + GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices().length + " screens");
        for (GraphicsDevice gd : GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()) {
            System.out.println("  Screen: " + gd.getIDstring() + " " + gd.getDisplayMode().getWidth() + "x" + gd.getDisplayMode().getHeight());
        }
        System.out.println("Current thread: " + Thread.currentThread().getName());
        System.out.println("Is EDT: " + SwingUtilities.isEventDispatchThread());
        System.out.println("-- Creating JFrame --");
        JFrame frame = new JFrame("KeycePass Test Window");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 300);
        frame.setLocationRelativeTo(null);
        frame.setAlwaysOnTop(true);
        frame.setVisible(true);
        System.out.println("Frame created: " + frame.isVisible() + " " + frame.getBounds());
        Thread.sleep(5000);
        System.out.println("Frame still visible: " + frame.isVisible());
        frame.dispose();
        System.out.println("-- Test complete --");
    }
}
