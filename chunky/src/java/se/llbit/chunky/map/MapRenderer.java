package se.llbit.chunky.map;


import javafx.scene.canvas.GraphicsContext;
import se.llbit.chunky.world.WorldView;

/**
 * Used to render the 2d map view on the interface
 */
public interface MapRenderer {
  /**
   * Render the section of the map denoted by the WorldView to the GraphicsContext
   * @param gc
   * @param view
   */
  void renderMap(GraphicsContext gc, WorldView view);
}
