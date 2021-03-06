/* Copyright (c) 2012 Jesper Öqvist <jesper@llbit.se>
 *
 * This file is part of Chunky.
 *
 * Chunky is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Chunky is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with Chunky.  If not, see <http://www.gnu.org/licenses/>.
 */
package se.llbit.chunky.model;

import se.llbit.chunky.renderer.scene.Scene;
import se.llbit.chunky.resources.Texture;
import se.llbit.chunky.world.BlockData;
import se.llbit.math.DoubleSidedQuad;
import se.llbit.math.Quad;
import se.llbit.math.QuickMath;
import se.llbit.math.Ray;
import se.llbit.math.Vector3;
import se.llbit.math.Vector4;

public class VineModel {

  protected static final Quad[] quads = {
      // North
      new DoubleSidedQuad(new Vector3(0, 0, 0.8 / 16), new Vector3(1, 0, 0.8 / 16),
          new Vector3(0, 1, 0.8 / 16), new Vector4(0, 1, 0, 1)),

      // South
      new DoubleSidedQuad(new Vector3(1, 0, 15.2 / 16), new Vector3(0, 0, 15.2 / 16),
          new Vector3(1, 1, 15.2 / 16), new Vector4(1, 0, 0, 1)),

      // East
      new DoubleSidedQuad(new Vector3(15.2 / 16, 0, 0), new Vector3(15.2 / 16, 0, 1),
          new Vector3(15.2 / 16, 1, 0), new Vector4(0, 1, 0, 1)),

      // West
      new DoubleSidedQuad(new Vector3(0.8 / 16, 0, 1), new Vector3(0.8 / 16, 0, 0),
          new Vector3(0.8 / 16, 1, 1), new Vector4(1, 0, 0, 1)),

      // Top
      new DoubleSidedQuad(new Vector3(0, 15.2 / 16, 0), new Vector3(1, 15.2 / 16, 0),
          new Vector3(0, 15.2 / 16, 1), new Vector4(0, 1, 0, 1)),
  };

  public static boolean intersect(Ray ray, Scene scene) {
    int data = ray.getBlockData();
    boolean hit = false;
    ray.t = Double.POSITIVE_INFINITY;
    for (int i = 0; i < quads.length; ++i) {
      if ((data & (1 << i)) != 0) {
        Quad quad = quads[i];
        if (quad.intersect(ray)) {
          float[] color = Texture.vines.getColor(ray.u, ray.v);
          if (color[3] > Ray.EPSILON) {
            ray.color.set(color);
            float[] biomeColor = ray.getBiomeFoliageColor(scene);
            ray.color.x *= biomeColor[0];
            ray.color.y *= biomeColor[1];
            ray.color.z *= biomeColor[2];
            ray.t = ray.tNext;
            ray.n.set(quad.n);
            ray.n.scale(QuickMath.signum(-ray.d.dot(quad.n)));
            hit = true;
          }
        }
      }
    }
    if (data == 0 || (ray.getCurrentData() & (1 << BlockData.VINE_TOP)) != 0) {
      Quad quad = quads[4];
      if (quad.intersect(ray)) {
        float[] color = Texture.vines.getColor(ray.u, ray.v);
        if (color[3] > Ray.EPSILON) {
          ray.color.set(color);
          float[] biomeColor = ray.getBiomeFoliageColor(scene);
          ray.color.x *= biomeColor[0];
          ray.color.y *= biomeColor[1];
          ray.color.z *= biomeColor[2];
          ray.t = ray.tNext;
          ray.n.set(quad.n);
          ray.n.scale(QuickMath.signum(-ray.d.dot(quad.n)));
          hit = true;
        }
      }
    }
    if (hit) {
      ray.distance += ray.t;
      ray.o.scaleAdd(ray.t, ray.d);
    }
    return hit;
  }

  public static boolean intersect(Ray ray, Scene scene, int connections) {
    boolean hit = false;
    ray.t = Double.POSITIVE_INFINITY;
    for (int i = 0; i < quads.length; ++i) {
      if ((connections & (1 << i)) != 0) {
        Quad quad = quads[i];
        if (quad.intersect(ray)) {
          float[] color = Texture.vines.getColor(ray.u, ray.v);
          if (color[3] > Ray.EPSILON) {
            ray.color.set(color);
            float[] biomeColor = ray.getBiomeFoliageColor(scene);
            ray.color.x *= biomeColor[0];
            ray.color.y *= biomeColor[1];
            ray.color.z *= biomeColor[2];
            ray.t = ray.tNext;
            ray.n.set(quad.n);
            ray.n.scale(QuickMath.signum(-ray.d.dot(quad.n)));
            hit = true;
          }
        }
      }
    }
    if (hit) {
      ray.distance += ray.t;
      ray.o.scaleAdd(ray.t, ray.d);
    }
    return hit;
  }
}
