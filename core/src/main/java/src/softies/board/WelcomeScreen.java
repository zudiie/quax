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

// draws the welcome screen: full-screen octagon wallpaper background with a centred layout
// shows QUAX title, "Human vs Bot" subtitle, and two buttons: Play Game and Quit
public class WelcomeScreen {

    private final Viewport viewport;
    private final OrthographicCamera camera;

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

    // tile size matches the game board world-unit size (128px * 0.25)
    private static final float TILE = 32f;

    private static final float PLAY_BTN_W = 260f;
    private static final float PLAY_BTN_H =  52f;

    private Rectangle playBounds;
    // quit is text-only; quitBounds is a generous invisible hit target
    private Rectangle quitBounds;

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

    public void draw(SpriteBatch batch, BitmapFont font,
                     BitmapFont welcomeFont, BitmapFont welcomeMiniFont,
                     ShapeRenderer sr, Vector3 mouse) {
        computeBounds();
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        drawBaseBackground(sr);
        drawOctagonWallpaper(sr);
        drawButtonShapes(sr, mouse);
        Gdx.gl.glDisable(GL20.GL_BLEND);
        batch.begin();
        drawText(batch, font, welcomeFont, welcomeMiniFont, mouse);
        batch.end();
    }

    public WelcomeAction handleInput(Vector3 touchPos) {
        if (playBounds != null && playBounds.contains(touchPos.x, touchPos.y))
            return WelcomeAction.START;
        if (quitBounds != null && quitBounds.contains(touchPos.x, touchPos.y))
            return WelcomeAction.QUIT_CONFIRM;
        return WelcomeAction.NONE;
    }

    public enum WelcomeAction { NONE, START, QUIT_CONFIRM }

    private void drawBaseBackground(ShapeRenderer sr) {
        float vw   = viewport.getWorldWidth();
        float vh   = viewport.getWorldHeight();
        float left = camera.position.x - vw / 2f;
        float bot  = camera.position.y - vh / 2f;
        sr.setProjectionMatrix(camera.combined);
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(BG_BASE);
        sr.rect(left, bot, vw, vh);
        sr.end();
    }

    private void drawOctagonWallpaper(ShapeRenderer sr) {
        float vw      = viewport.getWorldWidth();
        float vh      = viewport.getWorldHeight();
        float gridLeft = camera.position.x - vw / 2f - TILE;
        float gridBot  = camera.position.y - vh / 2f - TILE;
        float cut = TILE * 0.25f;
        int cols = (int) Math.ceil(vw / TILE) + 2;
        int rows = (int) Math.ceil(vh / TILE) + 2;

        sr.setProjectionMatrix(camera.combined);
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(GRID_OCT);
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                fillOctTile(sr, gridLeft + c * TILE, gridBot + r * TILE, TILE, cut);
        sr.end();

        sr.begin(ShapeRenderer.ShapeType.Line);
        sr.setColor(GRID_EDGE);
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                outlineOctTile(sr, gridLeft + c * TILE, gridBot + r * TILE, TILE, cut);
        sr.end();
    }

    private void drawButtonShapes(ShapeRenderer sr, Vector3 mouse) {
        sr.setProjectionMatrix(camera.combined);
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(playBounds.contains(mouse.x, mouse.y) ? BTN_HOV : BTN_FILL);
        sr.rect(playBounds.x, playBounds.y, playBounds.width, playBounds.height);
        sr.end();

        sr.begin(ShapeRenderer.ShapeType.Line);
        sr.setColor(COPPER);
        sr.rect(playBounds.x, playBounds.y, playBounds.width, playBounds.height);
        sr.end();
    }

    private void drawText(SpriteBatch batch, BitmapFont font,
                          BitmapFont welcomeFont, BitmapFont welcomeMiniFont, Vector3 mouse) {
        float cx = camera.position.x;
        float cy = camera.position.y;

        welcomeFont.getData().setScale(0.42f);
        welcomeFont.setColor(CREAM);
        GlyphLayout titleLayout = new GlyphLayout(welcomeFont, "QUAX");
        welcomeFont.draw(batch, "QUAX", cx - titleLayout.width / 2f, cy + 130f);
        welcomeFont.getData().setScale(0.2f);

        welcomeMiniFont.setColor(CARAMEL);
        GlyphLayout subLayout = new GlyphLayout(welcomeMiniFont, "Human  vs  Bot");
        welcomeMiniFont.draw(batch, "Human  vs  Bot", cx - subLayout.width / 2f, cy + 16f);

        font.setColor(CREAM);
        drawCentred(batch, font, "Play Game", playBounds);

        font.setColor(quitBounds.contains(mouse.x, mouse.y) ? QUIT_HOV : QUIT_COL);
        GlyphLayout quitLayout = new GlyphLayout(font, "Quit");
        font.draw(batch, "Quit", cx - quitLayout.width / 2f, cy - 108f);

        font.setColor(Color.WHITE);
    }

    private void fillOctTile(ShapeRenderer sr, float ox, float oy, float tile, float cut) {
        float[] v = octVerts(ox, oy, tile, cut);
        float fcx = ox + tile / 2f;
        float fcy = oy + tile / 2f;
        for (int i = 0; i < 8; i++) {
            int j = (i + 1) % 8;
            sr.triangle(fcx, fcy, v[i*2], v[i*2+1], v[j*2], v[j*2+1]);
        }
    }

    private void outlineOctTile(ShapeRenderer sr, float ox, float oy, float tile, float cut) {
        float[] v = octVerts(ox, oy, tile, cut);
        for (int i = 0; i < 8; i++) {
            int j = (i + 1) % 8;
            sr.line(v[i*2], v[i*2+1], v[j*2], v[j*2+1]);
        }
    }

    private float[] octVerts(float ox, float oy, float tile, float cut) {
        return new float[]{
            ox + cut,        oy,
            ox + tile - cut, oy,
            ox + tile,       oy + cut,
            ox + tile,       oy + tile - cut,
            ox + tile - cut, oy + tile,
            ox + cut,        oy + tile,
            ox,              oy + tile - cut,
            ox,              oy + cut
        };
    }

    private void computeBounds() {
        float cx = camera.position.x;
        float cy = camera.position.y;
        playBounds = new Rectangle(cx - PLAY_BTN_W / 2f, cy - 82f, PLAY_BTN_W, PLAY_BTN_H);
        quitBounds = new Rectangle(cx - 60f, cy - 130f, 120f, 36f);
    }

    private void drawCentred(SpriteBatch batch, BitmapFont font, String text, Rectangle r) {
        GlyphLayout gl = new GlyphLayout(font, text);
        font.draw(batch, text,
            r.x + (r.width  - gl.width)  / 2f,
            r.y + (r.height + gl.height) / 2f);
    }
}
