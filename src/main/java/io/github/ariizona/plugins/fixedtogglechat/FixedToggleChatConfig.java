package io.github.ariizona.plugins.fixedtogglechat;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("fixedtogglechat")
public interface FixedToggleChatConfig extends Config {
    @ConfigItem (
            position = 0,
            keyName = "resizeViewport",
            name = "Resize viewport",
            description = "Toggles viewport resizing on open/closed chat"
    )

    default boolean resizeViewport() {
        return true;
    }
}
