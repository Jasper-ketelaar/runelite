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
package net.runelite.client.plugins.mta.enchantment;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;
import javax.inject.Inject;
import com.google.common.eventbus.Subscribe;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemID;
import net.runelite.api.Tile;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ConfigChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemLayerChanged;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.plugins.mta.MTAConfig;
import net.runelite.client.plugins.mta.MTAPlugin;
import net.runelite.client.plugins.mta.MTARoom;

public class EnchantmentRoom extends MTARoom
{

	private static final int IMAGE_START_ID = 4;
	private static final int IMAGE_END_ID = 12;

	private final MTAPlugin plugin;
	private final Client client;
	private final Set<WorldPoint> dragonstones = new HashSet<>();

	private BufferedImage image;

	@Inject
	public EnchantmentRoom(MTAConfig config, MTAPlugin plugin, Client client)
	{
		super(config);
		this.plugin = plugin;
		this.client = client;
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{

		if (!inside() || !getConfig().enchantment())
		{
			return;
		}


		if (getConfig().enchantment())
		{
			setHiddenWidgets(true);
		}

		WorldPoint nearest = null;
		double dist = Double.MAX_VALUE;
		WorldPoint local = client.getLocalPlayer().getWorldLocation();
		for (WorldPoint worldPoint : dragonstones)
		{
			double currDist = local.distanceTo(worldPoint);
			if (nearest == null || currDist < dist)
			{
				dist = currDist;
				nearest = worldPoint;
			}
		}


		if (nearest != null)
		{
			client.setHintArrow(nearest);
		}
	}

	@Subscribe
	public void onItemLayerChanged(ItemLayerChanged event)
	{
		Tile changed = event.getTile();
		WorldPoint worldPoint = changed.getWorldLocation();
		for (Item item : changed.getGroundItems())
		{
			if (item.getId() == ItemID.DRAGONSTONE_6903)
			{
				dragonstones.add(worldPoint);
				return;
			}
		}

		if (dragonstones.contains(worldPoint))
		{
			dragonstones.remove(worldPoint);
		}
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
