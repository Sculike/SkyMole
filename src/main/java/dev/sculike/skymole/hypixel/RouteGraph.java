package dev.sculike.skymole.hypixel;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

import com.google.gson.Gson;

/**
 * A walking network for an island: points you can stand on and the paths between them
 * (some one-way, like dropping down a ledge). Finds the fastest walking route between two places.
 * Dwarven Mines network based on SkyHanni-REPO (MIT), see markers-LICENSE.txt.
 */
public final class RouteGraph {
	private record Raw(double[][] nodes, double[][] edges) {}

	private final double[][] nodes;
	private final int[][] next;
	private final double[][] cost;

	RouteGraph(double[][] nodes, double[][] edges) {
		this.nodes = nodes;
		int n = nodes.length;
		int[] count = new int[n];
		for (double[] e : edges) count[(int) e[0]]++;
		next = new int[n][];
		cost = new double[n][];
		for (int i = 0; i < n; i++) {
			next[i] = new int[count[i]];
			cost[i] = new double[count[i]];
		}
		int[] fill = new int[n];
		for (double[] e : edges) {
			int a = (int) e[0];
			next[a][fill[a]] = (int) e[1];
			cost[a][fill[a]] = e[2];
			fill[a]++;
		}
	}

	/** Loads a bundled network, e.g. "/assets/skymole/routes/dwarven_mines.json", or null. */
	public static RouteGraph load(String resource) {
		try (InputStream in = RouteGraph.class.getResourceAsStream(resource)) {
			if (in == null) return null;
			Raw raw = new Gson().fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), Raw.class);
			return new RouteGraph(raw.nodes(), raw.edges());
		} catch (Exception e) {
			return null;
		}
	}

	private int nearest(double x, double y, double z) {
		int best = -1;
		double bestDist = Double.MAX_VALUE;
		for (int i = 0; i < nodes.length; i++) {
			double dx = nodes[i][0] - x, dy = nodes[i][1] - y, dz = nodes[i][2] - z;
			double d = dx * dx + dy * dy * 4 + dz * dz; // height counts more: other floors aren't "near"
			if (d < bestDist) {
				bestDist = d;
				best = i;
			}
		}
		return best;
	}

	/**
	 * The fastest walking route from one place to another as a list of points {x, y, z},
	 * starting at the network point nearest {@code from} and ending at the one nearest {@code to}.
	 * Returns an empty list when there's no way through the network.
	 */
	public List<double[]> route(double[] from, double[] to) {
		int start = nearest(from[0], from[1], from[2]);
		int goal = nearest(to[0], to[1], to[2]);
		if (start < 0 || goal < 0) return List.of();

		double[] dist = new double[nodes.length];
		int[] prev = new int[nodes.length];
		Arrays.fill(dist, Double.MAX_VALUE);
		Arrays.fill(prev, -1);
		dist[start] = 0;
		PriorityQueue<double[]> queue = new PriorityQueue<>((a, b) -> Double.compare(a[0], b[0]));
		queue.add(new double[] {0, start});
		while (!queue.isEmpty()) {
			double[] item = queue.poll();
			int at = (int) item[1];
			if (item[0] > dist[at]) continue;
			if (at == goal) break;
			for (int k = 0; k < next[at].length; k++) {
				int to2 = next[at][k];
				double d = dist[at] + cost[at][k];
				if (d < dist[to2]) {
					dist[to2] = d;
					prev[to2] = at;
					queue.add(new double[] {d, to2});
				}
			}
		}
		if (dist[goal] == Double.MAX_VALUE) return List.of();

		List<double[]> path = new ArrayList<>();
		for (int at = goal; at != -1; at = prev[at]) path.add(nodes[at]);
		Collections.reverse(path);
		return path;
	}
}
