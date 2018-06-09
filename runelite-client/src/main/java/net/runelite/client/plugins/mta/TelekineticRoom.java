/*
 * Copyright (c) 2018, Jasper Ketelaar <Jasper0781@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.plugins.mta;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;
import javax.inject.Inject;
import com.google.common.eventbus.Subscribe;
import net.runelite.api.Client;
import net.runelite.api.CollisionDataFlag;
import net.runelite.api.GameState;
import net.runelite.api.GroundObject;
import net.runelite.api.NPC;
import net.runelite.api.NpcID;
import net.runelite.api.ObjectID;
import net.runelite.api.Perspective;
import net.runelite.api.Projectile;
import net.runelite.api.ProjectileID;
import net.runelite.api.WallObject;
import net.runelite.api.coords.Direction;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.ProjectileMoved;
import net.runelite.api.queries.GroundObjectQuery;
import net.runelite.api.queries.WallObjectQuery;
import net.runelite.api.widgets.WidgetID;

public class TelekineticRoom extends MTARoom
{
	private final Client client;

	private Stack<Direction> moves = new Stack<>();
	private LocalPoint destination;
	private LocalPoint location;
	private WorldPoint nearest;
	private Rectangle bounds;
	private NPC guardian;
	private Maze maze;
	private boolean valid;

	@Inject
	public TelekineticRoom(MTAConfig config, Client client)
	{
		super(config);
		this.client = client;
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (!getConfig().telekinetic()
				|| !inside()
				|| client.getGameState() != GameState.LOGGED_IN)
		{
			maze = null;
			moves.clear();
			client.clearHintArrow();
			return;
		}

		WallObjectQuery qry = new WallObjectQuery()
				.idEquals(ObjectID.TELEKINETIC_WALL);
		WallObject[] result = qry.result(client);
		int length = result.length;

		if (maze == null || length != maze.getWalls())
		{
			getBounds(result);
			maze = Maze.fromWalls(length);
			client.clearHintArrow();
		}
		else if (guardian != null)
		{
			if (guardian.getId() == NpcID.MAZE_GUARDIAN_MOVING)
			{
				destination = getGuardianDestination();
			}
			else
			{
				destination = null;
			}

			if (!moves.isEmpty())
			{
				valid = validatePosition(moves.peek());
			}

			//Prevent unnecessary updating when the guardian has not moved
			LocalPoint current = guardian.getLocalLocation();

			if (current.equals(location))
			{
				return;
			}

			location = current;

			if (location.equals(finish()))
			{
				client.clearHintArrow();
			}
			else
			{
				for (Projectile projectile : client.getProjectiles())
				{
					if (projectile.getId() == ProjectileID.TELEKINETIC_SPELL)
					{
						return;
					}
				}

				this.moves = build();
			}

		}
		else
		{
			client.clearHintArrow();
			moves.clear();
		}
	}

	@Subscribe
	public void onProjectileMoved(ProjectileMoved event)
	{
		Projectile projectile = event.getProjectile();

		if (valid)
		{
			moves.pop();
		}
		else
		{
			if (projectile.getId() == ProjectileID.TELEKINETIC_SPELL)
			{
				Direction mine = getPosition();

				if (mine != null)
				{
					LocalPoint local = neighbour(guardian.getLocalLocation(), mine);
					this.moves = build(WorldPoint.fromLocal(client, local));
				}
			}
		}
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned event)
	{
		NPC npc = event.getNpc();

		if (npc.getId() == NpcID.MAZE_GUARDIAN)
		{
			guardian = npc;
		}
	}


	@Subscribe
	public void onNpcDespawned(NpcDespawned event)
	{
		NPC npc = event.getNpc();

		if (npc.getId() == NpcID.MAZE_GUARDIAN)
		{
			guardian = null;
		}
	}

	@Override
	public boolean inside()
	{
		return client.getWidget(WidgetID.MTA_TELEKINETIC_GROUP_ID, 0) != null;
	}

	@Override
	public void under(Graphics2D graphics2D)
	{
		if (inside() && maze != null && guardian != null)
		{
			if (destination != null)
			{
				graphics2D.setColor(Color.ORANGE);
				renderLocalPoint(graphics2D, destination);
			}
			if (!moves.isEmpty())
			{
				if (valid)
				{
					graphics2D.setColor(Color.GREEN);
				}
				else
				{
					graphics2D.setColor(Color.RED);
				}


				Polygon tile = Perspective.getCanvasTilePoly(client, guardian.getLocalLocation());
				if (tile != null)
				{
					graphics2D.drawPolygon(tile);
				}


				WorldPoint optimal = optimal();

				if (optimal != null)
				{
					client.setHintArrow(optimal);
					renderWorldPoint(graphics2D, optimal);
				}
			}
		}
	}

	private WorldPoint optimal()
	{
		WorldPoint current = client.getLocalPlayer().getWorldLocation();

		Direction next = moves.pop();
		WorldArea areaNext = getIndicatorLine(next);
		WorldPoint nearestNext = nearest(areaNext, current);

		if (moves.isEmpty())
		{
			moves.push(next);

			return nearestNext;
		}

		Direction after = moves.peek();
		moves.push(next);
		WorldArea areaAfter = getIndicatorLine(after);
		WorldPoint nearestAfter = nearest(areaAfter, nearestNext);

		return nearest(areaNext, nearestAfter);
	}

	private int manhattan(WorldPoint point1, WorldPoint point2)
	{
		return Math.abs(point1.getX() - point2.getX()) + Math.abs(point2.getY() - point1.getY());
	}

	private WorldPoint nearest(WorldArea area, WorldPoint worldPoint)
	{
		int dist = Integer.MAX_VALUE;
		WorldPoint nearest = null;

		for (WorldPoint areaPoint : area.toWorldPointList())
		{
			int currDist = manhattan(areaPoint, worldPoint);
			if (nearest == null || dist > currDist)
			{
				nearest = areaPoint;
				dist = currDist;
			}
		}

		return nearest;
	}


	private void renderWorldPoint(Graphics2D graphics, WorldPoint worldPoint)
	{
		renderLocalPoint(graphics, LocalPoint.fromWorld(client, worldPoint));
	}

	private void renderLocalPoint(Graphics2D graphics, LocalPoint local)
	{
		if (local != null)
		{
			Polygon canvasTilePoly = Perspective.getCanvasTilePoly(client, local);
			if (canvasTilePoly != null)
			{
				graphics.drawPolygon(canvasTilePoly);
			}
		}
	}


	private Stack<Direction> build()
	{
		if (guardian.getId() == NpcID.MAZE_GUARDIAN_MOVING)
		{
			WorldPoint converted = WorldPoint.fromLocal(client, getGuardianDestination());
			return build(converted);
		}
		else
		{
			return build(guardian.getWorldLocation());
		}
	}

	private LocalPoint getGuardianDestination()
	{
		Direction facing = Direction.fromOrientation(guardian.getOrientation());
		return neighbour(guardian.getLocalLocation(), facing);
	}

	private Stack<Direction> build(WorldPoint start)
	{
		LocalPoint finish = finish();

		Queue<WorldPoint> visit = new LinkedList<>();
		Set<WorldPoint> closed = new HashSet<>();
		HashMap<WorldPoint, Integer> scores = new HashMap<>();
		HashMap<WorldPoint, WorldPoint> edges = new HashMap<>();
		scores.put(start, 0);
		visit.add(start);

		while (!visit.isEmpty())
		{
			WorldPoint next = visit.poll();
			closed.add(next);

			LocalPoint localNext = LocalPoint.fromWorld(client, next);
			LocalPoint[] neighbours = neighbours(localNext);

			for (LocalPoint neighbour : neighbours)
			{
				if (neighbour == null)
				{
					continue;
				}

				WorldPoint nghbWorld = WorldPoint.fromLocal(client, neighbour);

				if (!nghbWorld.equals(next)
						&& !closed.contains(nghbWorld))
				{
					int score = scores.get(next) + 1;

					if (!scores.containsKey(nghbWorld) || scores.get(nghbWorld) > score)
					{
						scores.put(nghbWorld, score);
						edges.put(nghbWorld, next);
						visit.add(nghbWorld);
					}
				}
			}
		}

		return build(edges, WorldPoint.fromLocal(client, finish));
	}

	private Stack<Direction> build(HashMap<WorldPoint, WorldPoint> edges, WorldPoint finish)
	{
		Stack<Direction> path = new Stack<>();
		WorldPoint current = finish;

		while (edges.containsKey(current))
		{
			WorldPoint next = edges.get(current);

			if (next.getX() > current.getX())
			{
				path.add(Direction.WEST);
			}
			else if (next.getX() < current.getX())
			{
				path.add(Direction.EAST);
			}
			else if (next.getY() > current.getY())
			{
				path.add(Direction.SOUTH);
			}
			else
			{
				path.add(Direction.NORTH);
			}

			current = next;
		}

		return path;
	}

	private LocalPoint[] neighbours(LocalPoint point)
	{
		return new LocalPoint[]
				{
						neighbour(point, Direction.NORTH), neighbour(point, Direction.SOUTH),
						neighbour(point, Direction.EAST), neighbour(point, Direction.WEST)
				};
	}

	private LocalPoint neighbour(LocalPoint point, Direction direction)
	{
		if (point == null)
		{
			return null;
		}

		int dx = 0, dy = 0;

		switch (direction)
		{
			case NORTH:
				dx = 0;
				dy = 1;
				break;
			case SOUTH:
				dx = 0;
				dy = -1;
				break;
			case EAST:
				dx = 1;
				dy = 0;
				break;
			case WEST:
				dx = -1;
				dy = 0;
				break;
		}

		int[][] flags = client.getCollisionMaps()[client.getPlane()].getFlags();
		int x = point.getRegionX();
		int y = point.getRegionY();

		int current = flags[x][y];
		int next = flags[x + dx][y + dy];

		while (!isBlocked(current, next, direction))
		{
			x += dx;
			y += dy;
			current = next;
			next = flags[x + dx][y + dy];
		}

		return client.getRegion().getTiles()[client.getPlane()][x][y].getLocalLocation();
	}

	private boolean isBlocked(int from, int to, Direction direction)
	{
		boolean blocked = checkFlag(to, CollisionDataFlag.BLOCK_MOVEMENT_FLOOR)
				|| checkFlag(to, CollisionDataFlag.BLOCK_MOVEMENT_OBJECT);

		if (blocked)
		{
			return true;
		}
		switch (direction)
		{
			case NORTH:
				return checkFlag(from, CollisionDataFlag.BLOCK_MOVEMENT_NORTH)
						|| checkFlag(to, CollisionDataFlag.BLOCK_MOVEMENT_SOUTH);
			case SOUTH:
				return checkFlag(from, CollisionDataFlag.BLOCK_MOVEMENT_SOUTH)
						|| checkFlag(to, CollisionDataFlag.BLOCK_MOVEMENT_NORTH);
			case EAST:
				return checkFlag(from, CollisionDataFlag.BLOCK_MOVEMENT_EAST)
						|| checkFlag(to, CollisionDataFlag.BLOCK_MOVEMENT_WEST);
			case WEST:
				return checkFlag(from, CollisionDataFlag.BLOCK_MOVEMENT_WEST)
						|| checkFlag(to, CollisionDataFlag.BLOCK_MOVEMENT_EAST);
		}

		return false;
	}

	private boolean checkFlag(int flag, int mask)
	{
		return (flag & mask) == mask;
	}

	private LocalPoint finish()
	{
		GroundObjectQuery qry = new GroundObjectQuery()
				.idEquals(ObjectID.TELEKINETIC_FINISH);

		GroundObject[] result = qry.result(client);

		if (result.length > 0)
		{
			return result[0].getLocalLocation();
		}

		return null;
	}

	private void getBounds(WallObject[] walls)
	{
		int minX = Integer.MAX_VALUE;
		int minY = Integer.MAX_VALUE;

		int maxX = Integer.MIN_VALUE;
		int maxY = Integer.MIN_VALUE;

		for (WallObject wall : walls)
		{
			WorldPoint point = wall.getWorldLocation();
			minX = Math.min(minX, point.getX());
			minY = Math.min(minY, point.getY());

			maxX = Math.max(maxX, point.getX());
			maxY = Math.max(maxY, point.getY());
		}

		this.bounds = new Rectangle(minX, minY, maxX - minX, maxY - minY);
	}

	private boolean validatePosition(Direction direction)
	{
		return direction == getPosition();
	}

	private Direction getPosition()
	{
		WorldPoint mine = client.getLocalPlayer().getWorldLocation();

		if (mine.getY() >= bounds.getMaxY() && mine.getX() < bounds.getMaxX() && mine.getX() > bounds.getX())
		{
			return Direction.NORTH;
		}
		else if (mine.getY() <= bounds.getY() && mine.getX() < bounds.getMaxX() && mine.getX() > bounds.getX())
		{
			return Direction.SOUTH;
		}
		else if (mine.getX() >= bounds.getMaxX() && mine.getY() < bounds.getMaxY() && mine.getY() > bounds.getY())
		{
			return Direction.EAST;
		}
		else if (mine.getX() <= bounds.getX() && mine.getY() < bounds.getMaxY() && mine.getY() > bounds.getY())
		{
			return Direction.WEST;
		}

		return null;
	}

	private WorldArea getIndicatorLine(Direction direction)
	{
		switch (direction)
		{
			case NORTH:
				return new WorldArea(bounds.x + 1, (int) bounds.getMaxY(), bounds.width - 1, 1, 0);
			case SOUTH:
				return new WorldArea(bounds.x + 1, bounds.y, bounds.width - 1, 1, 0);
			case WEST:
				return new WorldArea(bounds.x, bounds.y + 1, 1, bounds.height - 1, 0);
			case EAST:
				return new WorldArea((int) bounds.getMaxX(), bounds.y + 1, 1, bounds.height - 1, 0);
		}

		return null;
	}
}
