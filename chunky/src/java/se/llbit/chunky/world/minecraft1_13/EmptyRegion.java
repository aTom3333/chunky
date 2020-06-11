package se.llbit.chunky.world.minecraft1_13;

import java.util.Iterator;

public class EmptyRegion implements Region {
  private EmptyRegion() {
  }

  public static final EmptyRegion instance = new EmptyRegion();

  @Override
  public boolean isEmpty() {
    return true;
  }

  @Override
  public Iterator<Chunk> iterator() {
    return new Iterator<Chunk>() {
      @Override
      public boolean hasNext() {
        return false;
      }

      @Override
      public Chunk next() {
        return null;
      }
    };
  }
}
