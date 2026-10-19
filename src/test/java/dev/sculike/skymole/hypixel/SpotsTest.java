package dev.sculike.skymole.hypixel;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;

class SpotsTest {
	private static Spots.Spot at(int x, int y, int z) {
		return new Spots.Spot(MiningIsland.DWARVEN_MINES, "Test Area", false, false, new BlockPos(x, y, z));
	}

	@Test
	void heartIsTheMiddlePointNotAnEdge() {
		// Points along the area, nearest-to-you would be an end; the heart is the middle one.
		List<Spots.Spot> points = List.of(at(0, 100, 0), at(10, 100, 0), at(20, 100, 0), at(30, 100, 0), at(40, 100, 0));
		assertEquals(new BlockPos(20, 100, 0), Spots.heartOf(points).pos());
	}

	@Test
	void heartIsAlwaysARealPoint() {
		// An L-shaped area: the exact middle is outside it, so the closest real point is used.
		List<Spots.Spot> points = List.of(at(0, 100, 0), at(0, 100, 30), at(30, 100, 30));
		Spots.Spot heart = Spots.heartOf(points);
		assertEquals(true, points.contains(heart));
	}
}
