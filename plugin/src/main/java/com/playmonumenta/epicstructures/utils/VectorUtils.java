package com.playmonumenta.epicstructures.utils;

import org.bukkit.util.Vector;

public class VectorUtils {

	// rotate vector "position" by angle "degrees" on the x-z-(2D)-plane (yaw; compass direction)
	public static Vector rotateYAxis(Vector position, double degrees) {
		double radians = Math.toRadians(degrees);
		double x = position.getX();
		double y = position.getY();
		double z = position.getZ();
		double cos = Math.cos(radians);
		double sin = Math.sin(radians);

		return (new Vector(x * cos - z * sin, y, z * cos + x * sin));
	}

	// rotate vector "position" by angle "degrees" on the y-z-(2D)-plane (pitch; looking up/down)
	public static Vector rotateXAxis(Vector position, double degrees) {
		// Angle is offset, since:
		// In Minecraft,     0 is straight ahead, -90 is straight up, and  90 is straight down
		// In this formula, 90 is straight ahead,   0 is straight up, and 180 is straight down
		double radians = Math.toRadians(degrees);
		double x = position.getX();
		double y = position.getY();
		double z = position.getZ();
		double cos = Math.cos(radians);
		double sin = Math.sin(radians);

		return (new Vector(x, y * cos + z * sin, z * cos - y * sin));
	}

	// rotate vector "position" by angle "degrees" on the x-y-(2D)-plane (roll; turn your screen, basically)
	public static Vector rotateZAxis(Vector position, double degrees) {
		double radians = Math.toRadians(degrees);
		double x = position.getX();
		double y = position.getY();
		double z = position.getZ();
		double cos = Math.cos(radians);
		double sin = Math.sin(radians);
		return (new Vector(x * cos + y * sin, y * cos - x * sin, z));
	}

}
