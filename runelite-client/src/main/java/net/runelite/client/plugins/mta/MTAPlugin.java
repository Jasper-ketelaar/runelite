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
import java.awt.Rectangle;
import java.util.Arrays;
import java.util.Collection;
import javax.inject.Inject;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Provides;
import lombok.AccessLevel;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.events.ConfigChanged;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.Overlay;


@PluginDescriptor(name = "Mage Training Arena")
public class MTAPlugin extends Plugin
{
	public static final Color TRANSPARENT_GREEN = new Color(0, 255, 0, 102);
	public static final Color TRANSPARENT_ORANGE = new Color(255, 165, 0, 102);

	@Inject
	private AlchemyRoom alchemyRoom;
	@Inject
	private GraveyardRoom graveyardRoom;
	@Inject
	private TelekineticRoom telekineticRoom;
	@Inject
	private EnchantmentRoom enchantmentRoom;
	@Inject
	private Client client;
	@Inject
	private EventBus eventBus;
	@Inject
	private MTASceneOverlay sceneOverlay;
	@Inject
	private MTAInventoryOverlay inventoryOverlay;
	@Inject
	private MTAConfig config;
	@Getter(AccessLevel.PROTECTED)
	private MTARoom[] rooms;

	public void drawItem(Graphics2D graphics, WidgetItem item, Color border, Color fill)
	{
		Rectangle bounds = item.getCanvasBounds();
		graphics.setColor(border);
		graphics.draw(bounds);

		if (config.mtaFill())
		{
			graphics.setColor(fill);
			graphics.fill(bounds);
		}
	}

	@Provides
	public MTAConfig getConfig(ConfigManager manager)
	{
		return manager.getConfig(MTAConfig.class);
	}

	@Override
	public void startUp()
	{
		this.rooms = new MTARoom[] {alchemyRoom, graveyardRoom, telekineticRoom, enchantmentRoom};

		for (MTARoom room : rooms)
		{
			eventBus.register(room);
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged configChanged)
	{
		if (configChanged.getGroup().equals("mta")
				&& configChanged.getKey().equals("mtaHintArrows")
				&& configChanged.getNewValue().equals("false"))
		{
			client.clearHintArrow();
		}
	}

	@Override
	public void shutDown()
	{
		for (MTARoom room : rooms)
		{
			eventBus.unregister(room);
		}
	}

	@Override
	public Collection<Overlay> getOverlays()
	{
		return Arrays.asList(sceneOverlay, inventoryOverlay);
	}

}