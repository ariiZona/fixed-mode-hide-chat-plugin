package io.github.deathbeam.plugins.fixedhidechat;

import static io.github.deathbeam.plugins.fixedhidechat.FixedHideChatConstants.AUTO_EXPAND_WIDGETS;
import static io.github.deathbeam.plugins.fixedhidechat.FixedHideChatConstants.DEFAULT_VIEW_HEIGHT;
import static io.github.deathbeam.plugins.fixedhidechat.FixedHideChatConstants.EXPANDED_VIEW_HEIGHT;
import static io.github.deathbeam.plugins.fixedhidechat.FixedHideChatConstants.FIXED_MAIN;
import static io.github.deathbeam.plugins.fixedhidechat.FixedHideChatConstants.TO_CONTRACT_WIDGETS;
import java.awt.event.KeyEvent;
import java.util.Map;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.BeforeRender;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.KeyListener;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@Slf4j
@PluginDescriptor(
	name = "Fixed Mode Toggle Chat",
	description = "Allows to hide/show chatbox in fixed mode and size game accordingly"
)
public class FixedHideChatPlugin extends Plugin implements KeyListener
{
	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private KeyManager keyManager;

	private int lastMenu = 0;
	private boolean hideChat = true;

	@Override
	protected void startUp() throws Exception
	{
		// Register listener
		keyManager.registerKeyListener(this);
	}

	@Override
	protected void shutDown() throws Exception
	{
		// Unregister listener
		keyManager.unregisterKeyListener(this);

		// Reset menu state
		hideChat = true;
		lastMenu = 0;

		// Reset widgets
		clientThread.invoke(this::resetWidgets);
	}

	@Override
	public void keyTyped(KeyEvent e)
	{
		keyReleased(e);
	}

	@Override
	public void keyPressed(KeyEvent e)
	{
		keyReleased(e);
	}

	@Override
	public void keyReleased(KeyEvent e)
	{
		if (!client.isResized() && e.getKeyCode() == KeyEvent.VK_ESCAPE && !hideChat)
		{
			hideChat = true;
			e.consume();
		}
	}

	@Subscribe
	public void onClientTick(final ClientTick event) {
		if (client.isResized())
		{
			return;
		}

		// Bank container sometimes moves offscreen on resize and quick inputs, workaround
		final Widget widget = client.getWidget(WidgetInfo.BANK_CONTAINER);

		if (widget != null && !widget.isSelfHidden())
		{
			widget.setRelativeX(12);
			widget.setRelativeY(2);
		}
	}

	@Subscribe
	public void onBeforeRender(final BeforeRender event)
	{
		if (client.isResized())
		{
			return;
		}

		// Expand the view height
		setViewSizeTo(DEFAULT_VIEW_HEIGHT, EXPANDED_VIEW_HEIGHT);

		final Widget chatboxMessages = client.getWidget(WidgetInfo.CHATBOX);

		if (chatboxMessages != null)
		{
			boolean found = !hideChat;

			// Check if any auto-expand interface is open
			if (!found)
			{
				for (final Map.Entry<Integer, Integer> widgetInfo : AUTO_EXPAND_WIDGETS)
				{
					final Widget widget = client.getWidget(widgetInfo.getKey(), widgetInfo.getValue());

					if (widget != null && !widget.isSelfHidden())
					{
						found = true;
						break;
					}
				}
			}

			// Resize some widgets that might interfere with having expanded chat
			setWidgetsSizeTo(
				found ? EXPANDED_VIEW_HEIGHT : DEFAULT_VIEW_HEIGHT,
				found ? DEFAULT_VIEW_HEIGHT : EXPANDED_VIEW_HEIGHT);

			// Hide/show chat messages
			chatboxMessages.setHidden(!found);
		}
	}

	@Subscribe
	public void onMenuOptionClicked(final MenuOptionClicked event)
	{
		if (!"Switch tab".equals(event.getMenuOption()))
		{
			return;
		}

		final Widget chatboxMessages = client.getWidget(WidgetInfo.CHATBOX);
		final int newMenu = event.getWidgetId();
		hideChat = true;

		if (newMenu != lastMenu || (chatboxMessages != null && chatboxMessages.isHidden()))
		{
			hideChat = false;
			lastMenu = newMenu;
		}
	}

	private static void changeWidgetHeight(int originalHeight, int newHeight, Widget widget)
	{
		if (widget.getHeight() == originalHeight)
		{
			widget.setHeight(newHeight);

			final Widget[] nestedChildren = widget.getNestedChildren();

			if (nestedChildren != null)
			{
				for (final Widget nestedChild : nestedChildren)
				{
					if (nestedChild.getHeight() == originalHeight)
					{
						nestedChild.setHeight(newHeight);
					}
				}
			}

			final Widget[] dynamicChildren = widget.getDynamicChildren();

			if (dynamicChildren != null)
			{
				for (final Widget child : dynamicChildren)
				{
					if (child.getHeight() == originalHeight)
					{
						child.setHeight(newHeight);
					}
				}
			}
		}
	}

	private void setWidgetsSizeTo(final int originalHeight, final int newHeight)
	{
		for (final Map.Entry<Integer, Integer> widgetInfo : TO_CONTRACT_WIDGETS)
		{
			final Widget widget = client.getWidget(widgetInfo.getKey(), widgetInfo.getValue());

			if (widget != null && !widget.isSelfHidden())
			{
				changeWidgetHeight(originalHeight, newHeight, widget);
			}
		}
	}

	private void setViewSizeTo(final int originalHeight, final int newHeight)
	{
		final Widget viewport = client.getWidget(WidgetInfo.FIXED_VIEWPORT);

		if (viewport != null)
		{
			viewport.setHeight(newHeight);
		}

		final Widget fixedMain = client.getWidget(FIXED_MAIN.getKey(), FIXED_MAIN.getValue());

		if (fixedMain != null && fixedMain.getHeight() == originalHeight)
		{
			fixedMain.setHeight(newHeight);

			final Widget[] staticChildren = fixedMain.getStaticChildren();

			// Expand all children of the main fixed view
			for (final Widget child : staticChildren)
			{
				changeWidgetHeight(originalHeight, newHeight, child);
			}
		}

	}

	private void resetWidgets()
	{
		if (client.isResized())
		{
			return;
		}

		// Contract the view if it is expanded
		setViewSizeTo(EXPANDED_VIEW_HEIGHT, DEFAULT_VIEW_HEIGHT);
		setWidgetsSizeTo(EXPANDED_VIEW_HEIGHT, DEFAULT_VIEW_HEIGHT);

		// Show the chat messages widget again
		final Widget chatboxMessages = client.getWidget(WidgetInfo.CHATBOX);

		if (chatboxMessages != null)
		{
			chatboxMessages.setHidden(false);
		}
	}
}
