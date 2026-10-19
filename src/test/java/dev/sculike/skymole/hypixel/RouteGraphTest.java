package dev.sculike.skymole.hypixel;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class RouteGraphTest {
	// A square: 0 -> 1 -> 2 is short, 0 -> 3 -> 2 is long. 2 -> 0 is a one-way drop.
	private static final double[][] NODES = {{0, 100, 0}, {10, 100, 0}, {10, 100, 10}, {-50, 100, 10}};
	private static final double[][] EDGES = {
			{0, 1, 10}, {1, 0, 10}, {1, 2, 10}, {2, 1, 10},
			{0, 3, 50}, {3, 0, 50}, {3, 2, 60}, {2, 3, 60}};

	@Test
	void takesTheShortestWay() {
		RouteGraph g = new RouteGraph(NODES, EDGES);
		List<double[]> path = g.route(new double[] {0, 100, 1}, new double[] {10, 100, 11});
		assertEquals(3, path.size());
		assertArrayEquals(NODES[1], path.get(1));
	}

	@Test
	void respectsOneWayPaths() {
		double[][] edges = {{0, 1, 5}}; // only 0 -> 1, nothing back
		RouteGraph g = new RouteGraph(NODES, edges);
		assertFalse(g.route(NODES[0], NODES[1]).isEmpty());
		assertTrue(g.route(NODES[1], NODES[0]).isEmpty());
	}

	@Test
	void realDwarvenMinesNetworkConnectsTheCommissionSpots() {
		RouteGraph g = RouteGraph.load("/assets/skymole/routes/dwarven_mines.json");
		assertNotNull(g);
		// From the Dwarven Village to each of the measured commission spots.
		double[] village = {39, 202, -92};
		double[][] spots = {{50, 198, -15}, {36, 128, 33}, {113, 154, 40}, {0, 128, 150},
				{-73, 138, 150}, {-101, 148, 0}, {-109, 167, -70}};
		for (double[] spot : spots) {
			List<double[]> path = g.route(village, spot);
			assertFalse(path.isEmpty(), "no route to " + spot[0] + "," + spot[1] + "," + spot[2]);
		}
	}
}
