package se.llbit.chunky.world.minecraft1_13;

public class EmptyChunk implements Chunk {
  private EmptyChunk() {
  }

  public static final EmptyChunk instance = new EmptyChunk();

  @Override
  public boolean isEmpty() {
    return true;
  }
}
