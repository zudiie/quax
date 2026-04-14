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
import src.softies.GameMode;

// draws the welcome screen: title, subtitle, "Start Game" button and "Quit" button
// returns a WelcomeAction from handleInput() so Main can react without coupling here
public class WelcomeScreen {

    private final WorldCalculator world;
    private final Viewport viewport;
    private final OrthographicCamera camera;
    private final GameState gameState;
    private BitmapFont fontRef;

    private static final float OFFSET_Z = 300f;

    // computed each frame in draw()
    private Rectangle humanVsHumanBounds;
    private Rectangle humanVsBotBounds;
    private Rectangle startBounds;
    private Rectangle quitBounds;

    // mode-selection state for welcome screen feedback
    private boolean modeSelected = false;

    // start button — blue fill, gold border
    private static final Color START_IDLE  = new Color(0.14f, 0.24f, 0.62f, 1f);
    private static final Color START_HOVER = new Color(0.22f, 0.36f, 0.80f, 1f);
    // quit button — dark red fill, gold border
    private static final Color QUIT_IDLE   = new Color(0.32f, 0.07f, 0.07f, 1f);
    private static final Color QUIT_HOVER  = new Color(0.52f, 0.12f, 0.12f, 1f);
    private static final Color GOLD        = new Color(0.82f, 0.67f, 0.12f, 1f);

    private static final Color MODE_SELECTED = new Color(0.15f, 0.35f, 0.15f, 0.95f);
    private static final Color MODE_IDLE     = new Color(0.20f, 0.20f, 0.20f, 0.90f);
    private static final Color MODE_HOVER    = new Color(0.30f, 0.30f, 0.30f, 0.90f);

    public WelcomeScreen(WorldCalculator world, Viewport viewport, OrthographicCamera camera, GameState gameState) {
        this.world    = world;
        this.viewport = viewport;
        this.camera   = camera;
        this.gameState = gameState;
    }

    /**
     * draws the full welcome screen — manages batch begin/end internally
     * @param sr          ShapeRenderer for button backgrounds and borders
     * @param batch       SpriteBatch for text
     * @param font        small UI font for button labels
     * @param welcomeFont larger font for the title and subtitle
     */
    public void draw(SpriteBatch batch, BitmapFont font,
                     BitmapFont welcomeFont, ShapeRenderer sr, Vector3 mouse) {
        this.fontRef = font;
        computeButtonBounds();

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        drawButtonShapes(sr, mouse);

        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.begin();
        drawTitleText(batch, welcomeFont);
        drawButtonLabels(batch, font);
        batch.end();
    }

    /**
     * draws the filled button backgrounds and gold borders via ShapeRenderer
     */
    private void drawButtonShapes(ShapeRenderer sr, Vector3 mouse) {
        sr.setProjectionMatrix(camera.combined);

        sr.begin(ShapeRenderer.ShapeType.Filled);

        if (humanVsHumanBounds != null) {
            boolean hover = humanVsHumanBounds.contains(mouse.x, mouse.y);
            boolean selected = modeSelected && gameState.getGameMode() == GameMode.HUMAN_VS_HUMAN;
            sr.setColor(selected ? MODE_SELECTED : (hover ? MODE_HOVER : MODE_IDLE));
            sr.rect(humanVsHumanBounds.x, humanVsHumanBounds.y, humanVsHumanBounds.width, humanVsHumanBounds.height);
        }

        if (humanVsBotBounds != null) {
            boolean hover = humanVsBotBounds.contains(mouse.x, mouse.y);
            boolean selected = modeSelected && gameState.getGameMode() == GameMode.HUMAN_VS_BOT;
            sr.setColor(selected ? MODE_SELECTED : (hover ? MODE_HOVER : MODE_IDLE));
            sr.rect(humanVsBotBounds.x, humanVsBotBounds.y, humanVsBotBounds.width, humanVsBotBounds.height);
        }

        sr.setColor(startBounds.contains(mouse.x, mouse.y) ? START_HOVER : START_IDLE);
        sr.rect(startBounds.x, startBounds.y, startBounds.width, startBounds.height);

        sr.setColor(quitBounds.contains(mouse.x, mouse.y) ? QUIT_HOVER : QUIT_IDLE);
        sr.rect(quitBounds.x, quitBounds.y, quitBounds.width, quitBounds.height);
        sr.end();

        sr.begin(ShapeRenderer.ShapeType.Line);
        sr.setColor(GOLD);
        sr.rect(humanVsHumanBounds.x, humanVsHumanBounds.y, humanVsHumanBounds.width, humanVsHumanBounds.height);
        sr.rect(humanVsBotBounds.x, humanVsBotBounds.y, humanVsBotBounds.width, humanVsBotBounds.height);
        sr.rect(startBounds.x, startBounds.y, startBounds.width, startBounds.height);
        sr.rect(quitBounds.x,  quitBounds.y,  quitBounds.width,  quitBounds.height);
        sr.end();
    }

    /** draws the welcome title, subtitle, and selected-mode text in the right-side panel area */
    private void drawTitleText(SpriteBatch batch, BitmapFont welcomeFont) {
        welcomeFont.setColor(Color.WHITE);

        String title = "QUAX";
        GlyphLayout tl = new GlyphLayout(welcomeFont, title);
        welcomeFont.draw(batch, title,
            world.boardCenterX + (OFFSET_Z - tl.width) / 2f,
            world.boardCenterY + 200f);

        String sub = "Select Game Mode";
        GlyphLayout sl = new GlyphLayout(welcomeFont, sub);
        welcomeFont.draw(batch, sub,
            world.boardCenterX + (OFFSET_Z - sl.width) / 2f,
            world.boardCenterY + 150f);

        if (modeSelected) {
            String selectedText = "Selected: " +
                (gameState.getGameMode() == GameMode.HUMAN_VS_BOT ? "Human vs Bot" : "Human vs Human");
            GlyphLayout sel = new GlyphLayout(fontRef, selectedText);
            fontRef.draw(batch, selectedText,
                world.boardCenterX + (OFFSET_Z - sel.width) / 2f,
                world.boardCenterY + 115f);
        }
    }

    /** draws all four labels centred in their respective buttons */
    private void drawButtonLabels(SpriteBatch batch, BitmapFont font) {
        font.setColor(Color.WHITE);
        drawCentred(batch, font, "Human vs Human", humanVsHumanBounds);
        drawCentred(batch, font, "Human vs Bot",   humanVsBotBounds);
        drawCentred(batch, font, "Start Game",     startBounds);
        drawCentred(batch, font, "Quit",           quitBounds);
    }

    /**
     * processes a click on the welcome screen
     * @param touchPos world-space position of the click
     * @return what the user clicked (START, QUIT_CONFIRM, or NONE)
     */
    public WelcomeAction handleInput(Vector3 touchPos) {
        if (humanVsHumanBounds != null && humanVsHumanBounds.contains(touchPos.x, touchPos.y)) {
            modeSelected = true;
            return WelcomeAction.SELECT_HUMAN_VS_HUMAN;
        }
        if (humanVsBotBounds != null && humanVsBotBounds.contains(touchPos.x, touchPos.y)) {
            modeSelected = true;
            return WelcomeAction.SELECT_HUMAN_VS_BOT;
        }
        if (startBounds != null && startBounds.contains(touchPos.x, touchPos.y))
            return WelcomeAction.START;
        if (quitBounds  != null && quitBounds.contains(touchPos.x, touchPos.y))
            return WelcomeAction.QUIT_CONFIRM;
        return WelcomeAction.NONE;
    }

    // what the user did on the welcome screen
    public enum WelcomeAction {
        NONE,
        SELECT_HUMAN_VS_HUMAN,
        SELECT_HUMAN_VS_BOT,
        START,
        QUIT_CONFIRM
    }

    // -------------------------------------------------------------------------
    // helpers
    // -------------------------------------------------------------------------

    /** recomputes button rectangles — mode buttons above Start and Quit */
    private void computeButtonBounds() {
        float btnW = 240f;
        float btnH = 60f;
        float gap = 14f;
        float btnX = world.boardCenterX + (OFFSET_Z - btnW) / 2f;

        humanVsHumanBounds = new Rectangle(btnX, world.boardCenterY + 10f, btnW, btnH);
        humanVsBotBounds   = new Rectangle(btnX, world.boardCenterY - 64f, btnW, btnH);
        startBounds        = new Rectangle(btnX, world.boardCenterY - 152f, btnW, btnH);
        quitBounds         = new Rectangle(btnX, world.boardCenterY - 152f - btnH - gap, btnW, btnH);
    }

    private Vector3 getMouseWorldPos() {
        Vector3 mouse = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        viewport.unproject(mouse);
        return mouse;
    }

    private void drawCentred(SpriteBatch batch, BitmapFont font, String text, Rectangle r) {
        GlyphLayout gl = new GlyphLayout(font, text);
        font.draw(batch, text,
            r.x + (r.width  - gl.width)  / 2f,
            r.y + (r.height + gl.height) / 2f);
    }
}
