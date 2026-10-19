package dev.sculike.skymole.features;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class BestBlockTest {
	@Test
	void expectedBlocks() {
		assertEquals(1.0, BestBlock.expectedBlocks(0, 80));
		assertEquals(1.8, BestBlock.expectedBlocks(3, 80), 1e-9);   // 1 + 0.8 extra
		assertEquals(3.16, BestBlock.expectedBlocks(4, 216), 1e-9); // ~2 extra blocks at 216 spread
		assertEquals(3.0, BestBlock.expectedBlocks(2, 400), 1e-9);  // can't break more neighbors than it has
		assertEquals(1.0, BestBlock.expectedBlocks(4, 0));           // spread unknown
	}
}
