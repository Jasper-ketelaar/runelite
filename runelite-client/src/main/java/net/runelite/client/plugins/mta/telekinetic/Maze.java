package net.runelite.client.plugins.mta.telekinetic;

import java.awt.Rectangle;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.CollisionDataFlag;
import net.runelite.api.GroundObject;
import net.runelite.api.NPC;
import net.runelite.api.NpcID;
import net.runelite.api.ObjectID;
import net.runelite.api.WallObject;
import net.runelite.api.coords.Angle;
import net.runelite.api.coords.Direction;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.queries.GroundObjectQuery;

public class Maze
{
	private static final int TELEKINETIC_FINISH = ObjectID.NULL_23672;

	private final Client client;
	@Getter
	private final int[][] flags;
	@Getter
	private final WorldPoint finish;
	@Getter
	private final int walls;
	@Getter
	private Rectangle bounds;

	private MazeState state;

	private Maze(Client client, int[][] flags, int walls, WorldPoint finish, Rectangle bounds)
	{
		this.flags = flags;
		this.client = client;
		this.walls = walls;
		this.finish = finish;
	}

	public static Maze build(Client client, WallObject[] walls)
	{
		int[][] flags = client.getCollisionMaps()[client.getPlane()].getFlags();

		GroundObjectQuery gqry = new GroundObjectQuery()
			.idEquals(TELEKINETIC_FINISH);
		GroundObject[] groundObjects = gqry.result(client);
		if (groundObjects.length == 0)
		{
			return null;
		}

		WorldPoint finish = groundObjects[0].getWorldLocation();

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

		Rectangle bounds = new Rectangle(minX, minY, maxX - minX, maxY - minY);

		return new Maze(client, flags, walls.length, finish, bounds);
	}

	public int getFlag(int worldX, int worldY)
	{
		return this.flags[worldX - client.getBaseX()][worldY - client.getBaseY()];
	}

	public MazeState getState(NPC guardian)
	{
		if ((guardian.getId() == NpcID.MAZE_GUARDIAN_MOVING && getDestination(guardian).equals(state.getCurrent()))
			|| guardian.getId() == NpcID.MAZE_GUARDIAN && guardian.getWorldLocation().equals(state.getCurrent()))
		{
			return state;
		}

		return (state = new MazeState(this, guardian.getWorldLocation()));
	}

	public WorldArea getIndicatorLine(Direction direction)
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

	public Direction getMyDirection()
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

	private WorldPoint getDestination(NPC guardian)
	{
		Angle angle = new Angle(guardian.getOrientation());
		Direction facing = angle.getNearestDirection();
		return neighbour(guardian.getWorldLocation(), facing);
	}

	protected WorldPoint neighbour(WorldPoint point, Direction direction)
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

		int x = point.getX();
		int y = point.getY();
		int current = getFlag(x, y);
		int next = getFlags()[x + dx][y + dy];

		while (!isBlocked(current, next, direction))
		{
			x += dx;
			y += dy;
			current = next;
			next = getFlags()[x + dx][y + dy];
		}

		return new WorldPoint(x, y, 0);
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
}
