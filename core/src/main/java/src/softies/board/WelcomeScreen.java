package src.softies.board;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.Viewport;

// draws the welcome screen: full-screen octagon wallpaper background, centred layout
// shows QUAX title, "Human vs Bot" subtitle, and two buttons: Play Game / Quit
public class WelcomeScreen {

    private final Viewport viewport;
    private final OrthographicCamera camera;

    // - palette -
    private static final Color BG_BASE   = new Color(0.102f, 0.067f, 0.031f, 1f);
    private static final Color GRID_OCT  = new Color(0.180f, 0.102f, 0.039f, 0.30f);
    private static final Color GRID_EDGE = new Color(0.290f, 0.180f, 0.078f, 0.22f);
    private static final Color COPPER    = new Color(0.753f, 0.471f, 0.251f, 1f);
    private static final Color CREAM     = new Color(0.910f, 0.835f, 0.690f, 1f);
    private static final Color CARAMEL   = new Color(0.769f, 0.604f, 0.345f, 1f);
    private static final Color BTN_FILL  = new Color(0.243f, 0.145f, 0.063f, 1f);
    private static final Color BTN_HOV   = new Color(0.361f, 0.227f, 0.094f, 1f);
    private static final Color QUIT_COL  = new Color(0.580f, 0.435f, 0.251f, 1f);
    private static final Color QUIT_HOV  = new Color(0.769f, 0.604f, 0.345f, 1f);

    // - tile size - must match the game board's world-unit tile size (128px * 0.25) -
    private static final float TILE = 32f;

    // - button layout -
    private static final float PB_W = 260f; // Play Game width
    private static final float PB_H =  52f; // Play Game height
    private Rectangle playBounds;
    private Rectangle quitBounds; // generous hit target for the text-only quit

    /**
     * @param world     accepted for source-level compatibility; not used here
     * @param viewport  for full-screen sizing and coordinate conversions
     * @param camera    for ShapeRenderer projection and world-space coordinates
     * @param gameState accepted for source-level compatibility; not used here
     */
    public WelcomeScreen(WorldCalculator world, Viewport viewport,
                         OrthographicCamera camera, GameState gameState) {
        this.viewport = viewport;
        this.camera   = camera;
    }

    // - public draw - signature unchanged so Main.java needs no edits -

    public void draw(SpriteBatch batch, BitmapFont font,
                     BitmapFont welcomeFont, BitmapFont welcomeMiniFont, ShapeRenderer sr, Vector3 mouse) {
        computeBounds();

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        drawShapes(sr, mouse);

        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.begin();
        drawText(batch, font, welcomeFont, welcomeMiniFont, mouse);
        batch.end();
    }

    // - input -

    public WelcomeAction handleInput(Vector3 touchPos) {
        if (playBounds != null && playBounds.contains(touchPos.x, touchPos.y))
            return WelcomeAction.START;
        if (quitBounds != null && quitBounds.contains(touchPos.x, touchPos.y))
            return WelcomeAction.QUIT_CONFIRM;
        return WelcomeAction.NONE;
    }

    public enum WelcomeAction { NONE, START, QUIT_CONFIRM }

    private void drawShapes(ShapeRenderer sr, Vector3 mouse) {
        sr.setProjectionMatrix(camera.combined);

        float vw   = viewport.getWorldWidth();
        float vh   = viewport.getWorldHeight();
        float left = camera.position.x - vw / 2f;
        float bot  = camera.position.y - vh / 2f;
        float cx   = camera.position.x;
        float cy   = camera.position.y;

        sr.begin(ShapeRenderer.ShapeType.Filled);

        sr.setColor(BG_BASE);
        sr.rect(left, bot, vw, vh);

        float CUT = TILE * 0.25f;
        int colsNeeded = (int) Math.ceil(vw / TILE) + 2;
        int rowsNeeded = (int) Math.ceil(vh / TILE) + 2;
        float gridLeft = left - TILE;
        float gridBot  = bot  - TILE;

        sr.setColor(GRID_OCT);
        for (int row = 0; row < rowsNeeded; row++) {
            for (int col = 0; col < colsNeeded; col++) {
                float ox = gridLeft + col * TILE;
                float oy = gridBot  + row * TILE;
                fillOctTile(sr, ox, oy, TILE, CUT);
            }
        }

        boolean overPlay = playBounds.contains(mouse.x, mouse.y);
        sr.setColor(overPlay ? BTN_HOV : BTN_FILL);
        sr.rect(playBounds.x, playBounds.y, playBounds.width, playBounds.height);

        sr.end();

        sr.begin(ShapeRenderer.ShapeType.Line);

        sr.setColor(GRID_EDGE);
        for (int row = 0; row < rowsNeeded; row++) {
            for (int col = 0; col < colsNeeded; col++) {
                float ox = gridLeft + col * TILE;
                float oy = gridBot  + row * TILE;
                outlineOctTile(sr, ox, oy, TILE, CUT);
            }
        }

        float ruleY  = cy + 34f;
        float ruleHW = 100f;
        sr.setColor(COPPER);
        //sr.line(cx - ruleHW, ruleY, cx + ruleHW, ruleY);

        sr.setColor(COPPER);
        sr.rect(playBounds.x, playBounds.y, playBounds.width, playBounds.height);

        sr.end();
    }

    private void drawText(SpriteBatch batch, BitmapFont font,
                          BitmapFont welcomeFont, BitmapFont welcomeMiniFont, Vector3 mouse) {
        float cx = camera.position.x;
        float cy = camera.position.y;
        float vw = viewport.getWorldWidth();
        float vh = viewport.getWorldHeight();

        welcomeFont.getData().setScale(0.42f);
        welcomeFont.setColor(CREAM);
        String title = "QUAX";
        GlyphLayout tl = new GlyphLayout(welcomeFont, title);
        welcomeFont.draw(batch, title, cx - tl.width / 2f, cy + 130f);
        welcomeFont.getData().setScale(0.2f); // restore

        welcomeMiniFont.setColor(CARAMEL);
        String sub = "Human  vs  Bot";
        GlyphLayout sl = new GlyphLayout(welcomeMiniFont, sub);
        welcomeMiniFont.draw(batch, sub, cx - sl.width / 2f, cy + 16f);

        font.setColor(CREAM);
        drawCentred(batch, font, "Play Game", playBounds);

        boolean overQuit = quitBounds.contains(mouse.x, mouse.y);
        font.setColor(overQuit ? QUIT_HOV : QUIT_COL);
        String qLabel = "Quit";
        GlyphLayout ql = new GlyphLayout(font, qLabel);
        font.draw(batch, qLabel,
            cx - ql.width / 2f,
            cy - 108f);

        font.setColor(Color.WHITE); // reset
    }


    private void fillOctTile(ShapeRenderer sr, float ox, float oy,
                             float tile, float cut) {
        float[] v = octVerts(ox, oy, tile, cut);
        float fcx = ox + tile / 2f;
        float fcy = oy + tile / 2f;
        for (int i = 0; i < 8; i++) {
            int j = (i + 1) % 8;
            sr.triangle(fcx, fcy, v[i*2], v[i*2+1], v[j*2], v[j*2+1]);
        }
    }


    private void outlineOctTile(ShapeRenderer sr, float ox, float oy,
                                float tile, float cut) {
        float[] v = octVerts(ox, oy, tile, cut);
        for (int i = 0; i < 8; i++) {
            int j = (i + 1) % 8;
            sr.line(v[i*2], v[i*2+1], v[j*2], v[j*2+1]);
        }
    }


    private float[] octVerts(float ox, float oy, float tile, float cut) {
        return new float[]{
            ox + cut,        oy,               // bottom-left edge
            ox + tile - cut, oy,               // bottom-right edge
            ox + tile,       oy + cut,         // right-bottom
            ox + tile,       oy + tile - cut,  // right-top
            ox + tile - cut, oy + tile,        // top-right edge
            ox + cut,        oy + tile,        // top-left edge
            ox,              oy + tile - cut,  // left-top
            ox,              oy + cut          // left-bottom
        };
    }

    private void computeBounds() {
        float cx = camera.position.x;
        float cy = camera.position.y;

        // Play Game - centred, above the midpoint of the lower half
        playBounds = new Rectangle(cx - PB_W / 2f, cy - 82f, PB_W, PB_H);

        // Quit - generous invisible hit area around the text
        quitBounds = new Rectangle(cx - 60f, cy - 130f, 120f, 36f);
    }

    private void drawCentred(SpriteBatch batch, BitmapFont font,
                             String text, Rectangle r) {
        GlyphLayout gl = new GlyphLayout(font, text);
        font.draw(batch, text,
            r.x + (r.width  - gl.width)  / 2f,
            r.y + (r.height + gl.height) / 2f);
    }
}
