import java.awt.*;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class FontCustom{
    public static Font PressStart2P = null;
    public static void loadFonts(){
        try {
            InputStream is = FontCustom.class.getResourceAsStream("/fonts/kroe0555.ttf");
            if (is == null) throw new FileNotFoundException("Font file not found!");

            PressStart2P = Font.createFont(Font.TRUETYPE_FONT, is).deriveFont(Font.PLAIN, 8);
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(PressStart2P);
        } catch (IOException | FontFormatException e) {
            e.printStackTrace();
            PressStart2P = new Font("Monospaced", Font.PLAIN, 12);
        }
    }

}
