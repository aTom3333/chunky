package se.llbit.chunky.world.minecraft1_13;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import se.llbit.chunky.world.WorldView;

public class MapRenderer implements se.llbit.chunky.map.MapRenderer {
  private World world;

  public MapRenderer(World world) {
    this.world = world;
  }

  @Override
  public void renderMap(GraphicsContext gc, WorldView view) {
    double w = gc.getCanvas().getWidth();
    double h = gc.getCanvas().getHeight();
    Paint old = gc.getFill();
    gc.setFill(Color.BLUE); // For testing purpose
    gc.fillRect(0, 0, w, h);
    gc.setFill(old);
  }
}
