package se.llbit.chunky.world.minecraft1_13;

public interface Region extends Iterable<Chunk> {
  boolean isEmpty();
}
