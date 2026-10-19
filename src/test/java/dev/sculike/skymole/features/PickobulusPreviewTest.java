package dev.sculike.skymole.features;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PickobulusPreviewTest {
	private static final PickobulusPreview.Shape R2_ALL = new PickobulusPreview.Shape(2.0, false);

	@Test
	void picksTheShapeThatMatchedHypixel() {
		// Counts per shape in SHAPES order: r2 all, r2.5 all, r3 all, r1.5 all, r2 ores, r2.5 ores, r3 ores
		int[] counts = {33, 57, 123, 19, 12, 20, 41};
		assertEquals(R2_ALL, PickobulusPreview.bestShape(34, counts, R2_ALL));         // 33 vs 34: keep
		assertEquals(new PickobulusPreview.Shape(2.5, false), PickobulusPreview.bestShape(56, counts, R2_ALL));
		assertEquals(new PickobulusPreview.Shape(2.0, true), PickobulusPreview.bestShape(12, counts, R2_ALL));
	}

	@Test
	void tiesKeepTheCurrentShape() {
		int[] counts = {20, 57, 123, 19, 20, 20, 41};
		assertEquals(R2_ALL, PickobulusPreview.bestShape(20, counts, R2_ALL));
	}
}
