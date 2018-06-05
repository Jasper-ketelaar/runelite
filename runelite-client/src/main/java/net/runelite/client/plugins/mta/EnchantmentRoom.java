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

import java.awt.image.BufferedImage;
import javax.inject.Inject;
import com.google.common.eventbus.Subscribe;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemID;
import net.runelite.api.Player;
import net.runelite.api.Region;
import net.runelite.api.Tile;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ConfigChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.infobox.Counter;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;

public class EnchantmentRoom extends MTARoom
{

	private static final int IMAGE_START_ID = 4;
	private static final int IMAGE_END_ID = 12;

	private final InfoBoxManager infoBoxManager;
	private final ItemManager itemManager;
	private final MTAPlugin plugin;
	private final Client client;

	private Counter counter;
	private BufferedImage image;

	@Inject
	public EnchantmentRoom(MTAConfig config, MTAPlugin plugin, Client client, ItemManager itemManager, InfoBoxManager infoBoxManager)
	{
		super(config);
		this.plugin = plugin;
		this.client = client;
		this.itemManager = itemManager;
		this.infoBoxManager = infoBoxManager;
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (image == null)
		{
			image = itemManager.getImage(ItemID.DRAGONSTONE);
		}

		if (!inside() || !getConfig().enchantment())
		{
			if (this.counter != null)
			{
				infoBoxManager.removeIf(e -> e.getPlugin() instanceof MTAPlugin);
				this.counter = null;
			}

			return;
		}
		else if (counter == null)
		{
			this.counter = new Counter(image, plugin, "0");
			infoBoxManager.addInfoBox(counter);
		}

		if (getConfig().enchantment())
		{
			setHiddenWidgets(true);
		}

		Region region = client.getRegion();
		Tile[][][] tiles = region.getTiles();
		int z = client.getPlane();
		int count = 0;
		Player local = client.getLocalPlayer();
		WorldPoint nearest = null;
		double min = Double.MAX_VALUE;

		for (int x = 0; x < tiles[z].length; x++)
		{
			for (int y = 0; y < tiles[z][x].length; y++)
			{
				Tile tile = tiles[z][x][y];

				if (tile == null || tile.getGroundItems() == null)
				{
					continue;
				}

				double dist = local.getLocalLocation().distanceTo(tile.getLocalLocation());

				for (Item item : tile.getGroundItems())
				{
					if (item.getId() == ItemID.DRAGONSTONE_6903)
					{
						if (nearest == null || dist < min)
						{
							min = dist;
							nearest = tile.getWorldLocation();
						}

						count++;
					}
				}
			}
		}

		if (nearest != null && getConfig().mtaHintArrows())
		{
			client.setHintArrow(nearest);
		}

		this.counter.setText(String.valueOf(count));
		this.counter.setTooltip(String.format("%s dragonstones are spawned", count));
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (event.getGroup().equals("mta")
				&& event.getKey().equals("enchantment")
				&& event.getNewValue().equals("false")
				&& inside())
		{
			setHiddenWidgets(false);
		}
	}

	@Override
	public boolean inside()
	{
		Widget inside = client.getWidget(WidgetID.MTA_ENCHANTMENT_GROUP_ID, 0);

		return inside != null && !inside.isHidden();
	}

	public void setHiddenWidgets(boolean hide)
	{
		Widget text = client.getWidget(WidgetInfo.MTA_ENCHANTMENT_BONUS_TEXT);

		for (int i = IMAGE_START_ID; i < IMAGE_END_ID; i++)
		{
			Widget image = client.getWidget(WidgetID.MTA_ENCHANTMENT_GROUP_ID, i);

			if (image != null)
			{
				image.setHidden(hide);
			}
		}

		if (text != null)
		{
			text.setHidden(hide);
		}
	}
}
