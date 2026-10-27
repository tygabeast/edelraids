package com.tygabeast.edelraids.song;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Particle shapes in the element's color. Each call sends a bounded number of particles (at most 40). */
final class SongFx {
    private final ServerLevel level;
    private final DustParticleOptions dust;

    SongFx(ServerLevel level, int rgb) {
        this.level = level;
        this.dust = new DustParticleOptions(new Vector3f(((rgb >> 16) & 255) / 255f, ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f), 1.4f);
    }

    private void at(double x, double y, double z) {
        level.sendParticles(dust, x, y, z, 1, 0.05, 0.05, 0.05, 0.0);
    }

    void ring(Vec3 c, double radius, int points) {
        for (int i = 0; i < points; i++) {
            double a = i * Mth.TWO_PI / points;
            at(c.x + Math.cos(a) * radius, c.y + 0.2, c.z + Math.sin(a) * radius);
        }
    }

    void line(Vec3 from, Vec3 to) {
        Vec3 d = to.subtract(from);
        int n = Math.min(40, Math.max(2, (int) (d.length() * 2)));
        for (int i = 0; i <= n; i++) {
            Vec3 p = from.add(d.scale(i / (double) n));
            at(p.x, p.y, p.z);
        }
    }

    void cone(Vec3 eye, Vec3 look, double length) {
        Vec3 side = look.cross(new Vec3(0, 1, 0));
        side = side.lengthSqr() < 1e-4 ? new Vec3(1, 0, 0) : side.normalize();
        for (int i = 1; i <= 8; i++) {
            double dist = length * i / 8.0;
            double spread = dist * Math.tan(Math.toRadians(50));
            for (int j = -2; j <= 2; j++) {
                Vec3 p = eye.add(look.scale(dist)).add(side.scale(spread * j / 2.0));
                at(p.x, p.y - 0.3, p.z);
            }
        }
    }

    void spiral(Vec3 c, double radius, double height) {
        for (int i = 0; i < 24; i++) {
            double a = i * Mth.TWO_PI / 8;
            at(c.x + Math.cos(a) * radius, c.y + height * i / 24.0, c.z + Math.sin(a) * radius);
        }
    }
}
