package io.github.deathbeam.plugins.fixedtogglechat;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class FixedToggleChatPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(FixedToggleChatPlugin.class);
		RuneLite.main(args);
	}
}