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
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import javax.inject.Inject;
import com.google.common.eventbus.Subscribe;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.ItemComposition;
import net.runelite.api.ObjectID;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameTick;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;

public class AlchemyRoom extends MTARoom
{

	private static final int TEXT_Z_OFFSET = 180;
	private static final int IMAGE_Z_OFFSET = 150;
	private static final int CAPACITY = 8;
	private static final int INFO_START = 5;
	private static final int BEST_POINTS = 30;

	private static final List<Integer> CUPBOARDS = Arrays.asList(ObjectID.CUPBOARD_23678, ObjectID.CUPBOARD_23680,
			ObjectID.CUPBOARD_23682, ObjectID.CUPBOARD_23684, ObjectID.CUPBOARD_23686, ObjectID.CUPBOARD_23688,
			ObjectID.CUPBOARD_23690, ObjectID.CUPBOARD_23692);
	private static final String YOU_FOUND = "You found:";
	private static final String EMPTY = "The cupboard is empty.";

	private final HashMap<AlchemyItem, BufferedImage> IMAGE_MAP = new HashMap<>();
	private final AlchemyItem[] locations = new AlchemyItem[CAPACITY];
	private final List<GameObject> cupboards = new ArrayList<>(CAPACITY);

	private final MTAPlugin plugin;
	private final Client client;
	private final ItemManager itemManager;
	private final InfoBoxManager infoBoxManager;

	private String best;
	private int suggest = -1;

	@Inject
	public AlchemyRoom(Client client, MTAConfig config, MTAPlugin plugin, ItemManager itemManager,
					   InfoBoxManager infoBoxManager)
	{
		super(config);
		this.client = client;
		this.plugin = plugin;
		this.itemManager = itemManager;
		this.infoBoxManager = infoBoxManager;
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (!inside() || !getConfig().alchemy())
		{
			reset();
			return;
		}

		if (IMAGE_MAP.size() < AlchemyItem.values().length - 1)
		{
			for (AlchemyItem item : AlchemyItem.values())
			{
				if (item != AlchemyItem.EMPTY && !IMAGE_MAP.containsKey(item))
				{
					IMAGE_MAP.put(item, itemManager.getImage(item.getId()));
				}
			}
		}

		if (best == null || !best.equals(getBest()))
		{
			if (best != null)
			{
				infoBoxManager.removeIf(e -> e instanceof AlchemyRoomTimer);
				infoBoxManager.addInfoBox(new AlchemyRoomTimer(plugin));
			}

			best = getBest();
			reset();
		}

		if (getConfig().mtaHintArrows())
		{
			for (int i = 0; i < locations.length; i++)
			{
				AlchemyItem item = locations[i];
				if (item != null && item.toString().equals(best))
				{
					GameObject cupboard = cupboards.get(i);
					if (cupboard != null)
					{
						client.setHintArrow(cupboard.getWorldLocation());
						break;
					}
				}
			}
		}
	}


	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		if (!inside())
			return;

		GameObject spawn = event.getGameObject();
		for (int i = 0; i < CUPBOARDS.size(); i++)
		{
			int id = CUPBOARDS.get(i);
			if (spawn.getId() == id || spawn.getId() - 1 == id)
			{
				cupboards.add(i, spawn);
			}
		}

	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned event)
	{
		if (!inside())
			return;

		GameObject spawn = event.getGameObject();

		for (int i = 0; i < CUPBOARDS.size(); i++)
		{
			int id = CUPBOARDS.get(i);

			if (spawn.getId() == id || spawn.getId() - 1 == id)
			{
				cupboards.remove(i);
			}
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage wrapper)
	{
		if (!inside() || !getConfig().alchemy())
			return;

		String message = wrapper.getMessage();

		if (wrapper.getType() == ChatMessageType.SERVER)
		{
			if (message.contains(YOU_FOUND))
			{
				String item = message.replace(YOU_FOUND, "").trim();
				fill(getClicked(), item);
			}
			else if (message.equals(EMPTY))
			{
				locations[getClicked()] = AlchemyItem.EMPTY;
			}
		}
	}

	private void reset()
	{
		reset(false);
	}

	private void reset(boolean logout)
	{
		for (int i = 0; i < locations.length; i++)
		{
			locations[i] = AlchemyItem.UNKNOWN;
		}

		if (logout)
		{
			for (int i = 0; i < cupboards.size(); i++)
			{
				cupboards.set(i, null);
			}
		}
	}

	@Override
	public boolean inside()
	{
		return client.getWidget(WidgetID.MTA_ALCHEMY_GROUP_ID, 0) != null;
	}

	public String getBest()
	{
		for (int i = 0; i < INFO_START; i++)
		{
			int index = i + INFO_START;

			Widget textWidget = client.getWidget(WidgetID.MTA_ALCHEMY_GROUP_ID, index);

			if (textWidget == null)
			{
				reset();
				return "";
			}

			String item = textWidget.getText().replace(":", "");
			Widget pointsWidget = client.getWidget(WidgetID.MTA_ALCHEMY_GROUP_ID, index + INFO_START);
			int points = Integer.parseInt(pointsWidget.getText());

			if (points == BEST_POINTS)
			{
				return item;
			}
		}

		return "";
	}

	private int getClicked()
	{
		GameObject nearest = null;
		double distance = Double.MAX_VALUE;

		for (GameObject object : cupboards)
		{
			if (object == null)
			{
				continue;
			}

			WorldPoint mine = client.getLocalPlayer().getWorldLocation();
			double objectDistance = object.getWorldLocation().distanceTo(mine);

			if (nearest == null || distance > objectDistance)
			{
				nearest = object;
				distance = objectDistance;
			}
		}

		return cupboards.indexOf(nearest);
	}

	private void fill(int index, String item)
	{
		int distance = 8 - index;
		int start = (AlchemyItem.indexOf(item) + distance) % 8;

		for (int i = 0; i < CAPACITY; i++)
		{
			int itemIndex = (start + i) % 8;
			locations[i] = itemIndex > 4 ? AlchemyItem.EMPTY : AlchemyItem.values()[itemIndex];
		}
	}

	@Override
	public void under(Graphics2D graphics)
	{
		if (!getConfig().alchemy() || best == null || !inside())
		{
			return;
		}

		for (int i = 0; i < cupboards.size(); i++)
		{
			GameObject cupboard = cupboards.get(i);

			if (cupboard == null || i >= CAPACITY)
			{
				continue;
			}

			AlchemyItem location = locations[i];

			if (location == AlchemyItem.EMPTY)
				continue;

			String text = location.toString();

			if (text.equals(best))
			{
				if (getConfig().mtaHintArrows())
				{
					client.setHintArrow(cupboard.getWorldLocation());
				}
			}

			if (getConfig().alchemyIcon())
			{
				BufferedImage image = IMAGE_MAP.get(location);
				Point canvasLoc = Perspective.getCanvasImageLocation(client, graphics, cupboard.getLocalLocation(), image, IMAGE_Z_OFFSET);

				if (canvasLoc != null)
				{
					graphics.drawImage(image, canvasLoc.getX(), canvasLoc.getY(), null);

				}
			}
			else
			{
				Point canvasLoc = Perspective.getCanvasTextLocation(client, graphics, cupboard.getLocalLocation(), text, TEXT_Z_OFFSET);

				if (canvasLoc != null)
				{
					graphics.setColor(new Color(50, 50, 50));
					graphics.drawString(text, canvasLoc.getX() + 1, canvasLoc.getY() + 1);
					graphics.setColor(Color.WHITE);
					graphics.drawString(text, canvasLoc.getX(), canvasLoc.getY());
				}
			}
		}

		if (getConfig().alchemySuggest())
		{
			if (suggest == -1)
			{
				suggest = getClicked();
			}

			if (!suggest(suggest))
			{
				int index = (suggest + 3) % CAPACITY;

				if (!suggest(index))
				{
					suggest = -1;
				}
			}
		}
	}


	@Override
	public void over(Graphics2D graphics)
	{
		if (getConfig().alchemy())
		{
			if (!client.getWidget(WidgetInfo.INVENTORY).isHidden())
			{
				for (WidgetItem item : client.getWidget(WidgetInfo.INVENTORY).getWidgetItems())
				{
					ItemComposition composition = client.getItemDefinition(item.getId());
					String temp = best;

					if (temp.equals("Adamant Helm"))
					{
						temp = "adamant med helm";
					}

					if (composition != null && composition.getName().equalsIgnoreCase(temp))
					{
						plugin.drawItem(graphics, item, Color.GREEN, MTAPlugin.TRANSPARENT_GREEN);
					}
				}
			}
		}
	}

	private boolean suggest(int index)
	{
		AlchemyItem item = locations[index];

		if (item == AlchemyItem.UNKNOWN && getConfig().mtaHintArrows())
		{
			client.setHintArrow(cupboards.get(index).getWorldLocation());
			return true;
		}

		return false;
	}

}
