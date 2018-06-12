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
package net.runelite.client.plugins.mta.graveyard;

import java.awt.image.BufferedImage;
import javax.inject.Inject;
import com.google.common.eventbus.Subscribe;
import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.ItemID;
import net.runelite.api.events.ConfigChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.mta.MTAConfig;
import net.runelite.client.plugins.mta.MTAPlugin;
import net.runelite.client.plugins.mta.MTARoom;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;

public class GraveyardRoom extends MTARoom
{
	protected static final int MIN_SCORE = 16;
	private static final int BONES_START = 6903;
	private static final int BONES_TYPES = 4;
	private static final int INFO_WIDGET_START = 4;
	private static final int INFO_WIDGET_END = 11;

	private final Client client;
	private final MTAPlugin plugin;
	@Inject
	private ItemManager itemManager;
	@Inject
	private InfoBoxManager infoBoxManager;
	private Item[] items;
	private int score;
	private BufferedImage bonesImage;
	private GraveyardCounter counter;

	@Inject
	public GraveyardRoom(Client client, MTAConfig config, MTAPlugin plugin)
	{
		super(config);
		this.client = client;
		this.plugin = plugin;
	}

	@Override
	public boolean inside()
	{
		return client.getWidget(WidgetID.MTA_GRAVEYARD_GROUP_ID, 0) != null;
	}

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		if (this.bonesImage == null)
		{
			this.bonesImage = itemManager.getImage(ItemID.ANIMALS_BONES);
		}

		if (!inside() || !getConfig().graveyard())
		{
			if (this.counter != null)
			{
				infoBoxManager.removeIf(e -> e instanceof GraveyardCounter);
				this.counter = null;
			}

			return;
		}

		if (this.counter == null)
		{
			this.counter = new GraveyardCounter(bonesImage, plugin);
			this.score = score();
			this.counter.setCount(score);
			infoBoxManager.addInfoBox(counter);
		}

		if (getConfig().graveyard())
		{
			setHiddenWidgets(true);
		}
	}

	@Subscribe
	public void itemContainerChanged(ItemContainerChanged event)
	{
		ItemContainer container = event.getItemContainer();

		if (container == client.getItemContainer(InventoryID.INVENTORY))
		{
			this.items = container.getItems();
			this.score = score();

			if (this.counter != null)
			{
				this.counter.setCount(score);
			}
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (event.getGroup().equals("mta")
				&& event.getKey().equals("graveyard")
				&& event.getNewValue().equals("false")
				&& inside())
		{
			setHiddenWidgets(false);
		}
	}

	private int score()
	{
		int score = 0;

		for (Item item : items)
		{
			if (isBone(item.getId()))
			{
				score += item.getId() - BONES_START;
			}
		}

		return score;
	}

	private boolean isBone(int id)
	{
		int sub = id - BONES_START;
		return sub > 0 && sub <= BONES_TYPES;
	}

	public void setHiddenWidgets(boolean hide)
	{
		for (int i = INFO_WIDGET_START; i <= INFO_WIDGET_END; i++)
		{
			Widget image = client.getWidget(WidgetID.MTA_GRAVEYARD_GROUP_ID, i);

			if (image != null)
			{
				image.setHidden(hide);
			}
		}

	}
}