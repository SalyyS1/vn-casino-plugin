package vn.casino.gui.framework;

import com.github.stefvanschie.inventoryframework.gui.type.util.Gui;
import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.function.Consumer;

/**
 * Reusable button component for GUI items.
 * Provides a fluent API for creating interactive buttons with:
 * - Material icon
 * - MiniMessage-formatted name and lore
 * - Click handler
 */
public class GuiButton {

    private final ItemStack item;
    private final Consumer<org.bukkit.event.inventory.InventoryClickEvent> onClick;

    private GuiButton(ItemStack item, Consumer<org.bukkit.event.inventory.InventoryClickEvent> onClick) {
        this.item = item;
        this.onClick = onClick;
    }

    /**
     * Create a new button with MiniMessage formatting.
     *
     * @param material Button icon material
     * @param name Display name (supports MiniMessage)
     * @param lore Lore lines (supports MiniMessage)
     * @param onClick Click handler
     * @return GuiButton instance
     */
    public static GuiButton create(
        Material material,
        String name,
        List<String> lore,
        Consumer<org.bukkit.event.inventory.InventoryClickEvent> onClick
    ) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            // Parse MiniMessage for name
            Component nameComponent = MiniMessage.miniMessage().deserialize(name);
            meta.displayName(nameComponent);

            // Parse MiniMessage for lore
            if (lore != null && !lore.isEmpty()) {
                List<Component> loreComponents = lore.stream()
                    .map(line -> MiniMessage.miniMessage().deserialize(line))
                    .toList();
                meta.lore(loreComponents);
            }

            item.setItemMeta(meta);
        }

        return new GuiButton(item, onClick);
    }

    /**
     * Create button without click handler (display-only).
     *
     * @param material Button icon material
     * @param name Display name (supports MiniMessage)
     * @param lore Lore lines (supports MiniMessage)
     * @return GuiButton instance
     */
    public static GuiButton createDisplay(Material material, String name, List<String> lore) {
        return create(material, name, lore, e -> {});
    }

    /**
     * Convert to InventoryFramework GuiItem.
     *
     * @return GuiItem ready to add to pane
     */
    public GuiItem toGuiItem() {
        return new GuiItem(item, onClick);
    }

    /**
     * Get the underlying ItemStack.
     *
     * @return ItemStack
     */
    public ItemStack getItem() {
        return item.clone();
    }
}
