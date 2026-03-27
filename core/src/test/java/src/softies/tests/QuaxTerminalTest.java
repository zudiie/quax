package src.softies.tests;

import org.junit.*;
import src.softies.GameDisplay;
import src.softies.GameMode;
import src.softies.QuaxController;

import java.io.*;
import java.lang.reflect.Method;
import static org.junit.Assert.*;

/**
 * Additional tests for terminal interaction (start screen, mode selection, etc.).
 * Uses JUnit 4.
 */
public class QuaxTerminalTest {

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final InputStream originalIn = System.in;

    @Before
    public void setUpStreams() {
        outContent.reset();
        System.setOut(new PrintStream(outContent));
    }

    @After
    public void restoreStreams() {
        System.setOut(originalOut);
        System.setIn(originalIn);
    }

    @Test
    public void testDisplayStartScreen() {
        GameDisplay display = new GameDisplay();
        display.displayStartScreen();
        String output = outContent.toString();
        assertTrue(output.contains("Welcome to Quax Board!"));
        assertTrue(output.contains("Press Enter Key to Start!"));
        assertTrue(output.contains("type quit"));
    }

    @Test
    public void testDisplayModeSelection() {
        GameDisplay display = new GameDisplay();
        display.displayModeSelection();
        String output = outContent.toString();
        assertTrue(output.contains("Choose Game Mode:"));
        assertTrue(output.contains("Human vs Human"));
        assertTrue(output.contains("Human vs Bot"));
    }

    @Test
    public void testShowMessage() {
        GameDisplay display = new GameDisplay();
        display.showMessage("Test message");
        assertEquals(">> Test message", outContent.toString());
    }

    @Test
    public void testShowLoading() {
        GameDisplay display = new GameDisplay();
        display.showLoading("Loading");
        String output = outContent.toString();
        assertTrue(output.startsWith("Loading"));
        assertTrue(output.endsWith("...\n"));
    }

    @Test
    public void testStartScreenInputEnter() throws Exception {
        String input = "\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        QuaxController controller = new QuaxController();
        Method method = QuaxController.class.getDeclaredMethod("waitForEnterOrQuit");
        method.setAccessible(true);
        boolean result = (boolean) method.invoke(controller);
        assertTrue(result);
    }

    @Test
    public void testStartScreenInputQuit() throws Exception {
        String input = "quit\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        QuaxController controller = new QuaxController();
        Method method = QuaxController.class.getDeclaredMethod("waitForEnterOrQuit");
        method.setAccessible(true);
        boolean result = (boolean) method.invoke(controller);
        assertFalse(result);
    }

    @Test
    public void testModeSelectionHuman() throws Exception {
        String input = "human\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        QuaxController controller = new QuaxController();
        Method method = QuaxController.class.getDeclaredMethod("getGameModeInput");
        method.setAccessible(true);
        GameMode mode = (GameMode) method.invoke(controller);
        assertEquals(GameMode.HUMAN_VS_HUMAN, mode);
    }

    @Test
    public void testModeSelectionBot() throws Exception {
        String input = "bot\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        QuaxController controller = new QuaxController();
        Method method = QuaxController.class.getDeclaredMethod("getGameModeInput");
        method.setAccessible(true);
        GameMode mode = (GameMode) method.invoke(controller);
        assertEquals(GameMode.HUMAN_VS_BOT, mode);
    }

    @Test
    public void testModeSelectionInvalidThenValid() throws Exception {
        String input = "invalid\nhuman\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        QuaxController controller = new QuaxController();
        Method method = QuaxController.class.getDeclaredMethod("getGameModeInput");
        method.setAccessible(true);
        GameMode mode = (GameMode) method.invoke(controller);
        assertEquals(GameMode.HUMAN_VS_HUMAN, mode);
        String output = outContent.toString();
        assertTrue(output.contains("Invalid input"));
    }
}
