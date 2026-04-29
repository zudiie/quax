package src.softies;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;

// creates and returns the two fonts used in the game
// both are generated at high pixel size then scaled down for crisp rendering on any display
public class FontLoader {

    // file name of the font to load from assets
    //private static final String FONT_FILE = "CormorantGaramond-Bold.ttf";
    private static final String FONT_FILE = "SF-Pro-Display-Bold.ttf";

    // characters that must be present - DEFAULT_CHARS covers A-Z, 0-9, punctuation etc.
    private static final String REQUIRED_CHARS = FreeTypeFontGenerator.DEFAULT_CHARS + "•";

    /**
     * creates the main UI font used for board labels, objectives, buttons and status messages
     * generated at 100px then scaled to 0.2x so the glyph atlas stays sharp at any window size
     * @return the configured BitmapFont - caller is responsible for disposing it
     */
    public static BitmapFont loadMainFont() {
        FreeTypeFontGenerator gen = new FreeTypeFontGenerator(Gdx.files.internal(FONT_FILE));
        FreeTypeFontParameter p   = buildParams(100, 5);
        BitmapFont font = gen.generateFont(p);
        font.getData().setScale(0.2f);
        gen.dispose();
        return font;
    }

    /**
     * creates the larger welcome-screen font for the title and subtitle
     * generated at 110px (slightly smaller than before for a less overwhelming heading)
     * @return the configured BitmapFont - caller is responsible for disposing it
     */
    public static BitmapFont loadWelcomeFont() {
        FreeTypeFontGenerator gen = new FreeTypeFontGenerator(Gdx.files.internal(FONT_FILE));
        FreeTypeFontParameter p   = buildParams(220, 2);
        BitmapFont font = gen.generateFont(p);
        font.getData().setScale(0.1f);
        gen.dispose();
        return font;
    }

    public static BitmapFont loadWelcomeMiniFont() {
        FreeTypeFontGenerator gen = new FreeTypeFontGenerator(Gdx.files.internal(FONT_FILE));
        FreeTypeFontParameter p   = buildParams(180, 2);
        BitmapFont font = gen.generateFont(p);
        font.getData().setScale(0.15f);
        gen.dispose();
        return font;
    }

    /**
     * builds a FreeType parameter object with sensible defaults for the game's visual style
     * @param size        point size to rasterise at (before scaling)
     * @param borderWidth thickness of the black outline
     * @return a configured parameter object ready to pass to generateFont()
     */
    private static FreeTypeFontParameter buildParams(int size, int borderWidth) {
        FreeTypeFontParameter p = new FreeTypeFontParameter();
        p.size        = size;
        p.characters  = REQUIRED_CHARS;
        p.color       = Color.WHITE;
        p.kerning     = true;
        p.spaceX      = -1;
        p.borderColor = Color.BLACK;
        p.borderWidth = borderWidth;
        return p;
    }
}
