import java.awt.*;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class FontCustom{
    public static Font PressStart2P = null;
    public static void loadFonts(){
        try {
            // โหลดฟอนต์จาก resources/fonts
            InputStream is = FontCustom.class.getResourceAsStream("/fonts/PressStart2P.ttf");
            if (is == null) throw new FileNotFoundException("Font file not found!");

            // สร้างฟอนต์จากไฟล์
            PressStart2P = Font.createFont(Font.TRUETYPE_FONT, is).deriveFont(8f);
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(PressStart2P); // ลงทะเบียนฟอนต์
        } catch (IOException | FontFormatException e) {
            e.printStackTrace();
            // ถ้าไม่สามารถโหลดฟอนต์ได้ให้ใช้ fallback
            PressStart2P = new Font("Monospaced", Font.PLAIN, 12);
        }
    }

}