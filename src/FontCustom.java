import java.awt.*;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class FontCustom {
    public static Font MainFont = null;

    public static void loadFonts() {
        try {
            InputStream is = FontCustom.class.getResourceAsStream("/fonts/kroe0555.ttf");
            if (is == null) throw new FileNotFoundException("Font file not found!");

            MainFont = Font.createFont(Font.TRUETYPE_FONT, is).deriveFont(Font.PLAIN, 8);
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(MainFont);
        } catch (IOException | FontFormatException e) {
            e.printStackTrace();
            MainFont = new Font("Monospaced", Font.PLAIN, 12);
        }
    }

}
