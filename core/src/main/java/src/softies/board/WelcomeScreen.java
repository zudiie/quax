package src.softies.board;

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
import com.badlogic.gdx.Gdx;

// draws the welcome screen and handles its button input
// no background panel — just title, subtitle, and two styled buttons on the grey background
public class WelcomeScreen {

    private final WorldCalculator world;
    private final Viewport viewport;
    private final OrthographicCamera camera;

    private final float offsetZ = 300f;

    // button rects recomputed each frame
    private Rectangle startBounds;
    private Rectangle quitBounds;

    // start button: blue fill, gold border
    private static final Color START_IDLE  = new Color(0.14f, 0.24f, 0.62f, 1f);
    private static final Color START_HOVER = new Color(0.22f, 0.36f, 0.80f, 1f);
    // quit button: dark red fill, gold border
    private static final Color QUIT_IDLE   = new Color(0.32f, 0.07f, 0.07f, 1f);
    private static final Color QUIT_HOVER  = new Color(0.52f, 0.12f, 0.12f, 1f);
    private static final Color GOLD        = new Color(0.82f, 0.67f, 0.12f, 1f);

    /**
     * @param world    board bounds for button positioning
     * @param viewport for world-size queries and mouse unprojecting
     * @param camera   for the ShapeRenderer projection matrix
     */
    public WelcomeScreen(WorldCalculator world, Viewport viewport, OrthographicCamera camera) {
        this.world    = world;
        this.viewport = viewport;
        this.camera   = camera;
    }

    /**
     * recomputes the two button rectangles — call once per frame before draw() and handleInput()
     */
    public void updateBounds() {
        float btnW = 220, btnH = 60, gap = 14;
        float btnX = world.boardCenterX + (offsetZ - btnW) / 2f;
        startBounds = new Rectangle(btnX, world.boardCenterY - 30f,          btnW, btnH);
        quitBounds  = new Rectangle(btnX, world.boardCenterY - 30f - btnH - gap, btnW, btnH);
    }

    /**
     * draws title, subtitle, start button and quit button
     * @param shapeRenderer for button backgrounds and borders
     * @param batch         for text (must not be active when called — this method manages begin/end)
     * @param font          small UI font for button labels
     * @param welcomeFont   larger font for title and subtitle
     */
    public void draw(ShapeRenderer shapeRenderer, SpriteBatch batch,
                     BitmapFont font, BitmapFont welcomeFont) {
        updateBounds();

        Vector3 mouse = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        viewport.unproject(mouse);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.setProjectionMatrix(camera.combined);

        // button backgrounds
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(startBounds.contains(mouse.x, mouse.y) ? START_HOVER : START_IDLE);
        shapeRenderer.rect(startBounds.x, startBounds.y, startBounds.width, startBounds.height);

        shapeRenderer.setColor(quitBounds.contains(mouse.x, mouse.y) ? QUIT_HOVER : QUIT_IDLE);
        shapeRenderer.rect(quitBounds.x, quitBounds.y, quitBounds.width, quitBounds.height);
        shapeRenderer.end();

        // gold borders
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(GOLD);
        shapeRenderer.rect(startBounds.x, startBounds.y, startBounds.width, startBounds.height);
        shapeRenderer.rect(quitBounds.x,  quitBounds.y,  quitBounds.width,  quitBounds.height);
        shapeRenderer.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);

        // text
        batch.begin();

        // "QUAX" — white, centred in the panel area
        welcomeFont.setColor(Color.WHITE);
        String title = "QUAX";
        GlyphLayout tl = new GlyphLayout(welcomeFont, title);
        welcomeFont.draw(batch, title,
            world.boardCenterX + (offsetZ - tl.width) / 2f,
            world.boardCenterY + 120f);

        // subtitle — white
        String sub = "Human vs Human";
        GlyphLayout sl = new GlyphLayout(welcomeFont, sub);
        welcomeFont.draw(batch, sub,
            world.boardCenterX + (offsetZ - sl.width) / 2f,
            world.boardCenterY + 80f);

        // start button label — centred in button
        font.setColor(Color.WHITE);
        drawCentred(batch, font, "Start Game", startBounds);
        drawCentred(batch, font, "Quit",       quitBounds);

        batch.end();
    }

    /**
     * draws text centred inside the given rectangle
     */
    private void drawCentred(SpriteBatch batch, BitmapFont font, String text, Rectangle rect) {
        GlyphLayout gl = new GlyphLayout(font, text);
        font.draw(batch, text,
            rect.x + (rect.width  - gl.width)  / 2f,
            rect.y + (rect.height + gl.height) / 2f);
    }

    /**
     * processes a click on the welcome screen
     * @param touchPos world-space click position
     * @return WelcomeAction indicating what the user did
     */
    public WelcomeAction handleInput(Vector3 touchPos) {
        if (startBounds != null && startBounds.contains(touchPos.x, touchPos.y))
            return WelcomeAction.START;
        if (quitBounds  != null && quitBounds.contains(touchPos.x, touchPos.y))
            return WelcomeAction.QUIT_CONFIRM;
        return WelcomeAction.NONE;
    }

    // what the user did on the welcome screen
    public enum WelcomeAction { NONE, START, QUIT_CONFIRM }
}
