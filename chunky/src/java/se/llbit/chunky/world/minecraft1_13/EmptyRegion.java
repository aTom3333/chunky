package se.llbit.chunky.world.minecraft1_13;

public class EmptyRegion implements Region {
  private EmptyRegion() {
  }

  public static final EmptyRegion instance = new EmptyRegion();

  @Override
  public boolean isEmpty() {
    return true;
  }
}
