package src.softies;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import java.util.ArrayList;
import java.util.List;

public class Main extends ApplicationAdapter {
    private TiledMap map;
    private OrthogonalTiledMapRenderer renderer;
    private OrthographicCamera camera;
    private Viewport viewport;
    private TiledMapTileLayer octagonLayer;

    private static final int VIRTUAL_WIDTH = 800;
    private static final int VIRTUAL_HEIGHT = 600;

    private ShapeRenderer shapeRenderer;
    private List<Vector3> clickedTiles = new ArrayList<>();
    private int tileWidth;
    private int tileHeight;
    private float unitScale = 0.25f;

    @Override
    public void create() {
        camera = new OrthographicCamera();
        camera.setToOrtho(false);

        viewport = new FitViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, camera);

        map = new TmxMapLoader().load("PolygonalGrid.tmx");

        renderer = new OrthogonalTiledMapRenderer(map, unitScale);

        MapLayer layer = map.getLayers().get("Octagons");
        if (layer == null) {
            layer = map.getLayers().get(0);
        }

        if (layer instanceof TiledMapTileLayer) {
            octagonLayer = (TiledMapTileLayer) layer;
        }

        tileWidth = map.getProperties().get("tilewidth", Integer.class);
        tileHeight = map.getProperties().get("tileheight", Integer.class);
        shapeRenderer = new ShapeRenderer();
        centerCameraOnMap();
    }

    private void centerCameraOnMap() {
        float mapWidth = map.getProperties().get("width", Integer.class) * tileWidth * unitScale;
        float mapHeight = map.getProperties().get("height", Integer.class) * tileHeight * unitScale;
        camera.position.set(mapWidth / 2f, mapHeight / 2f, 0);
        camera.update();
        viewport.apply();
    }

    @Override
    public void render() {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        handleInput();

        camera.update();

        renderer.setView(camera);
        renderer.render();

        renderBlackCircles();
    }

    private void handleInput() {
        if (Gdx.input.justTouched()) {
            // Get screen coordinates
            float screenX = Gdx.input.getX();
            float screenY = Gdx.input.getY();

            // Convert to world coordinates
            Vector3 worldPos = new Vector3(screenX, screenY, 0);
            camera.unproject(worldPos);

            int tileX = (int) (worldPos.x / (tileWidth * unitScale));
            int tileY = (int) (worldPos.y / (tileHeight * unitScale));

            float tileCenterX = (tileX * tileWidth * unitScale) + (tileWidth * unitScale) / 2f;
            float tileCenterY = (tileY * tileHeight * unitScale) + (tileHeight * unitScale) / 2f;

            clickedTiles.add(new Vector3(tileCenterX, tileCenterY, 0));

            System.out.println("Screen: (" + screenX + ", " + screenY + ")");
            System.out.println("World: (" + worldPos.x + ", " + worldPos.y + ")");
            System.out.println("Tile: (" + tileX + ", " + tileY + ")");
            System.out.println("Circle at: (" + tileCenterX + ", " + tileCenterY + ")");
        }
    }

    private void renderBlackCircles() {
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(Color.BLACK);

        // Draw a black circle for each clicked tile
        for (Vector3 tilePos : clickedTiles) {
            float radius = Math.min(tileWidth, tileHeight) * 0.35f * unitScale;
            shapeRenderer.circle(tilePos.x, tilePos.y, radius);
        }

        shapeRenderer.end();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void dispose() {
        map.dispose();
        renderer.dispose();
        shapeRenderer.dispose();
    }
}


//package src.softies;
///*
//    main class to start the quax software system.
// */
//public class Main {
//    public static void main(String[] args) {
//        // launches the controller which handles the entire game flow
//        QuaxController controller = new QuaxController();
//        controller.launch();
//    }
//}
