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
package net.runelite.client.plugins.mta.telekinetic;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import javax.inject.Inject;
import com.google.common.eventbus.Subscribe;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.NpcID;
import net.runelite.api.ObjectID;
import net.runelite.api.Perspective;
import net.runelite.api.VarPlayer;
import net.runelite.api.Varbits;
import net.runelite.api.WallObject;
import net.runelite.api.coords.Direction;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.queries.WallObjectQuery;
import net.runelite.api.widgets.WidgetID;
import net.runelite.client.plugins.mta.MTAConfig;
import net.runelite.client.plugins.mta.MTARoom;

public class TelekineticRoom extends MTARoom
{
	private static final int TELEKINETIC_WALL = ObjectID.NULL_10755;
	private static final int TELEKINETIC_FINISH = ObjectID.NULL_23672;

	private final Client client;

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
			client.clearHintArrow();
			return;
		}

		WallObjectQuery qry = new WallObjectQuery()
			.idEquals(TELEKINETIC_WALL);
		WallObject[] result = qry.result(client);
		int length = result.length;

		if (maze == null || length != maze.getWalls())
		{
			maze = Maze.build(client, result);
			client.clearHintArrow();
		}
		else if (guardian != null)
		{
			MazeState state = maze.getState(guardian);

			if (state.moves() == 0)
			{
				client.clearHintArrow();
			}
		}
		else
		{
			client.clearHintArrow();
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
			if (maze.getState(guardian).moves() > 0)
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

		MazeState state = maze.getState(guardian);
		WorldPoint nextPoint = state.getMove(0);
		Direction next = between(state.getCurrent(), nextPoint);
		WorldArea areaNext = maze.getIndicatorLine(next);
		WorldPoint nearestNext = nearest(areaNext, current);

		if (state.moves() == 1)
		{
			return nearestNext;
		}

		Direction after = between(nextPoint, state.getMove(1));
		WorldArea areaAfter = maze.getIndicatorLine(after);
		WorldPoint nearestAfter = nearest(areaAfter, nearestNext);

		return nearest(areaNext, nearestAfter);
	}

	private Direction between(WorldPoint from, WorldPoint to)
	{
		if (to.getX() > from.getX())
		{
			return Direction.EAST;
		}
		else if (to.getY() > from.getY())
		{
			return Direction.NORTH;
		}
		else if (to.getY() < from.getY())
		{
			return Direction.SOUTH;
		}

		return Direction.WEST;

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
}
