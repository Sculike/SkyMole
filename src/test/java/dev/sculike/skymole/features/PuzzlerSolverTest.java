package dev.sculike.skymole.features;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.Test;

class PuzzlerSolverTest {
	@Test
	void followsTheArrows() {
		assertArrayEquals(new int[] {181, 135}, PuzzlerSolver.solve(""));
		assertArrayEquals(new int[] {182, 137}, PuzzlerSolver.solve("▲▲◀"));      // up up left
		assertArrayEquals(new int[] {180, 134}, PuzzlerSolver.solve(" ▼▶ "));          // ignores spaces
		assertArrayEquals(new int[] {181, 135}, PuzzlerSolver.solve("▲▼◀▶")); // cancels out
	}
}
