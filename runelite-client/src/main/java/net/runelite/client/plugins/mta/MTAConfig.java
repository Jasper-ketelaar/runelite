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

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(
		keyName = "mta",
		name = "Mage Training Arena",
		description = "Configuration for the Mage Training Arena plugin"
)
public interface MTAConfig extends Config
{
	@ConfigItem(
			keyName = "mtaFill",
			name = "Fill inventory click boxes",
			description = "This will fill click boxes with a transparent colour instead of just outlining them",
			position = 0
	)

	default boolean mtaFill()
	{
		return false;
	}

	@ConfigItem(
			keyName = "mtaHintArrows",
			name = "Enable hint arrows",
			description = "Will draw hint arrows for the rooms that have hint arrow functionality",
			position = 1
	)

	default boolean mtaHintArrows()
	{
		return true;
	}

	@ConfigItem(
			keyName = "alchemy",
			name = "Enable alchemy room",
			description = "Configures whether or not the alchemy room overlay is enabled.",
			position = 2
	)

	default boolean alchemy()
	{
		return true;
	}

	@ConfigItem(
			keyName = "alchemyIcon",
			name = "Draw item icons on the cupboards",
			description = "Draws the item icons on the cupboards instead of the text",
			position = 3
	)

	default boolean alchemyIcon()
	{
		return true;
	}

	@ConfigItem(
			keyName = "alchemySuggest",
			name = "Suggest unknown cupboard",
			description = "Suggests the best cupboard to click on when the room resets and positions are unknown",
			position = 4
	)

	default boolean alchemySuggest()
	{
		return true;
	}

	@ConfigItem(
			keyName = "graveyard",
			name = "Enable graveyard room",
			description = "Configures whether or not the graveyard room overlay is enabled.",
			position = 5
	)

	default boolean graveyard()
	{
		return true;
	}

	@ConfigItem(
			keyName = "graveyardInventory",
			name = "Enable graveyard inventory overlay",
			description = "Will draw boxes over the inventory items that display ",
			position = 6
	)

	default boolean graveyardInventory()
	{
		return false;
	}

	@ConfigItem(
			keyName = "telekinetic",
			name = "Enable telekinetic room",
			description = "Configures whether or not the telekinetic room overlay is enabled.",
			position = 7
	)

	default boolean telekinetic()
	{
		return true;
	}

	@ConfigItem(
			keyName = "telekineticLines",
			name = "Draw wall lines of where to stand",
			description = "Will draw lines for the tiles that you are able to cast the spell from",
			position = 8
	)

	default boolean telekineticLines()
	{
		return false;
	}

	@ConfigItem(
			keyName = "enchantment",
			name = "Enable enchantment room",
			description = "Configures whether or not the enchantment room overlay is enabled.",
			position = 9
	)

	default boolean enchantment()
	{
		return true;
	}


}
