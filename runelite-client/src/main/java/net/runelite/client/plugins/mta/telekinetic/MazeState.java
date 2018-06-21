package net.runelite.client.plugins.mta.telekinetic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;
import lombok.Getter;
import net.runelite.api.coords.Direction;
import net.runelite.api.coords.WorldPoint;

public class MazeState
{
	private final Maze maze;
	@Getter
	private final WorldPoint current;
	@Getter
	private final List<WorldPoint> solution;

	public MazeState(Maze maze, WorldPoint current)
	{
		this.maze = maze;
		this.current = current;
		this.solution = solve();
	}

	public WorldPoint getMove(int move)
	{
		return solution.get(solution.size() - (1 + move));
	}

	public int moves()
	{
		return solution.size();
	}

	private List<WorldPoint> solve()
	{
		WorldPoint finish = maze.getFinish();

		Queue<WorldPoint> visit = new LinkedList<>();
		Set<WorldPoint> closed = new HashSet<>();
		HashMap<WorldPoint, Integer> scores = new HashMap<>();
		HashMap<WorldPoint, WorldPoint> edges = new HashMap<>();
		scores.put(current, 0);
		visit.add(current);

		while (!visit.isEmpty())
		{
			WorldPoint next = visit.poll();
			closed.add(next);

			WorldPoint[] neighbours = neighbours(next);

			for (WorldPoint neighbour : neighbours)
			{
				if (neighbour == null)
				{
					continue;
				}

				if (!neighbour.equals(next)
					&& !closed.contains(neighbour))
				{
					int score = scores.get(next) + 1;

					if (!scores.containsKey(neighbour) || scores.get(neighbour) > score)
					{
						scores.put(neighbour, score);
						edges.put(neighbour, next);
						visit.add(neighbour);
					}
				}
			}
		}

		ArrayList<WorldPoint> result = new ArrayList<>();
		WorldPoint current = finish;
		while (edges.containsKey(current))
		{
			result.add(current);
			current = edges.get(current);
		}

		return result;
	}

	private WorldPoint[] neighbours(WorldPoint point)
	{
		return new WorldPoint[]
			{
				maze.neighbour(point, Direction.NORTH), maze.neighbour(point, Direction.SOUTH),
				maze.neighbour(point, Direction.EAST), maze.neighbour(point, Direction.WEST)
			};
	}
}
