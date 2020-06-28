package se.llbit.chunky.world;


import se.llbit.chunky.map.MapRenderer;
import se.llbit.chunky.renderer.scene.Scene;

/**
 * This interface won't really be used (at least i don't think so atm)
 *
 * The plan is to completely abstract the world format and its specification.
 * Even the notion of chunks or regions should just be an implementation detail of the world format.
 * So every action that needs to be performed on a world needs to be defined in this interface
 *
 * This interface is not used in the code, it just serves as a reference for what is needed until
 * the real class that will be used for interacting with the world is defined
 */

/**
 * The world is considered to be an abstract source of blocks and entities in 3D space
 * Outside of the various World implementations and their associated classes, no
 * information about a world's structure is known and can be relied upon.
 */
public interface WorldInterface {
  /**
   * A world needs to preloaded, preferably asynchronously.
   *
   * preloading refers to the work that needs to done in order to show the
   * user the map so he can later select his region of interest and load it for real
   *
   * We don't want to leak implementation details such as the fact that the world
   * can be split into smaller units (be that regions, chunks or whatever) even if,
   * in practice, most world format should might be able to.
   * So we can't make it so that the preloading threads will query a world unit they have to
   * load.
   *
   * We don't want to reimplement the preloading threads for each world format either.
   *
   * A possible solution would be that the world can create preloading jobs, that
   * the preloading threads can take and execute. Those preloading jobs will be world specific
   * so they'll be able to use the knowledge of the world units without leaking it
   */
  PreLoadingJob nextPreLoadingJob();


  /**
   * In order to be able to meaningfully create PreLoadingJob, the world needs to know
   * what to preload, as such it needs to be notified when the map view changes
   *
   * Note: the types and names are temporary and just to give an idea of what is needed
   * the current MapView can't be used as it uses ChunkView which leak implementation detail
   * (and it isn't used like that, perhaps World needs to be listener?)
   * @param newView
   */
  void viewChanged(WorldView newView);


  /**
   * We need to find a way to describe a selection that isn't tied to the concept of chunk
   * and that isn't tied to 2D either
   *
   * This will need work in the UI as well as the selection will no longer use ChunkPosition
   * Perhaps this selection could be world specific? It would make things harder on the ui side
   * but also make it somewhat easier too for the implementations as it will be able to use chunk.
   *
   * For now i'm thinking more about some free selection tool that allow any shape in the XZ plane
   * and we also give it yMin yMax (we could add shortcut to select 16*16 blocks at the time
   * and snap to a 16*16 grid for those who want the chunk-like selection behavior
   * without leaking implementation detail).
   * Perhaps discriminating the Y axis is not a good idea and the selection should be more free
   * in this axis too?
   * Anyway, if we go for such a selection, we would need a better data structure for the selection,
   * a list of every blocks or even every 1*1 column wouldn't cut it.
   */
  void setSelection(Selection selection);


  /**
   * The world needs to be able to load the selection into the various data structures
   * needed for rendering.
   * The world could either:
   *    - take the data structures by reference and filling them
   *    - create the data structures itself and returning them
   *    - create loading jobs that will be executed asynchronously
   * If we want the asynchronous solution, data structure will need to either
   * be be thread safe when mutating, or be able to be partially build
   * separately and merged later
   *
   * I don't what solution is the best for now, i'll write solution 1 for now but that can change anytime
   */
  void loadSelection(Scene scene);


  /**
   * The map view needs to be rendered by the world
   *
   * Right now, there are 2 different map rendering, topography and biome.
   * Should biomes be considered to be an implementation detail or not?
   * Whatever we choose we will need to change how biome specific textures are handled
   *  Regarding the map view rendering, if biome are not an implementation detail (they are part
   *  of the public interface), we will make 2 map view rendering method. If
   *  it is an implementation detail, map view will need to encompass the zoom level
   *  to allow us to between topology and biome rendering with a single method
   *
   * A mapRenderer can be requested and the implementation will happen through this class
   */
  MapRenderer getMapRenderer();


  /**
   * There are more things that we might want the world to do, for those
   * things what is the best solution? Making them part of the interface or not
   *
   * For example things that we seem to have to make part of the interface
   * is the fact that a world has 3 dimensions.
   * What should we do? If the 3 dimensions are part of the interface, the ui
   * can stay as it is.
   * If they are not but the fact that there are dimensions that are identified by number
   * is part of the interface, the ui could use a number input to choose dimension but that is less user friendly
   * If dimension are not part of the interface, how do we expose them to the user
   * when they do exist?
   * A solution to this problem (and maybe some others) could be to use
   * the command design pattern to query additional information to certain kind of worlds
   */
}
