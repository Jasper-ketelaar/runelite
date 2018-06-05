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

import lombok.Getter;
import net.runelite.api.ItemID;

public enum AlchemyItem
{
	LEATHER_BOOTS("Leather boots", ItemID.LEATHER_BOOTS),
	ADAMANT_KITESHIELD("Adamant kiteshield", ItemID.ADAMANT_KITESHIELD),
	ADAMANT_MED_HELM("helm", ItemID.ADAMANT_MED_HELM),
	EMERALD("Emerald", ItemID.EMERALD),
	RUNE_LONGSWORD("Rune longsword", ItemID.RUNE_LONGSWORD),
	EMPTY("", -1),
	UNKNOWN("Unknown", ItemID.CAKE_OF_GUIDANCE);

	@Getter
	private final int id;
	@Getter
	private final String name;

	AlchemyItem(String name, int id)
	{
		this.id = id;
		this.name = name;
	}

	public static int indexOf(String item)
	{
		AlchemyItem[] items = values();

		for (int i = 0; i < items.length; i++)
		{
			if (item.contains(items[i].getName()))
			{
				return i;
			}
		}

		return -1;
	}

	@Override
	public String toString()
	{
		String text = getName();

		if (text.equals("helm"))
		{
			return "Adamant Helm";
		}
		else if (text.contains(" "))
		{
			String[] parts = text.split(" ");
			return parts[0] + " " + parts[1].substring(0, 1).toUpperCase() + parts[1].substring(1);
		}

		return text;
	}
}
