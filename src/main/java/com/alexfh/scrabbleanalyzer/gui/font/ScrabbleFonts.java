package com.alexfh.scrabbleanalyzer.gui.font;

import java.awt.*;
import java.io.IOException;
import java.util.Objects;

public class ScrabbleFonts {

    private static final String fontFolder = "/assets/fonts/";
    public static Font courierNew;
    public static Font courierNewBold;
    public static Font courierNewItalic;
    public static Font courierNewBoldItalic;

    public static void init() throws IOException, FontFormatException {
        ScrabbleFonts.courierNew = ScrabbleFonts.getTTFFontFromName("cour.ttf");
        ScrabbleFonts.courierNewBold = ScrabbleFonts.getTTFFontFromName("courbd.ttf");
        ScrabbleFonts.courierNewItalic = ScrabbleFonts.getTTFFontFromName("couri.ttf");
        ScrabbleFonts.courierNewBoldItalic = ScrabbleFonts.getTTFFontFromName("courbi.ttf");
    }

    private static Font getTTFFontFromName(String fileName) throws IOException, FontFormatException {
        return Font.createFont(
            Font.TRUETYPE_FONT,
            Objects.requireNonNull(
                ScrabbleFonts.class.getResourceAsStream(fontFolder + fileName)
            )
        );
    }

}
