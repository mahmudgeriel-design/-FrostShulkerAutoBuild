package my.frost.shulker;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.*;

public final class Main extends JavaPlugin implements Listener {
    private final String GUI_PREFIX = "§8Шалкер: ";
    private final Map<UUID, Integer> openShulkerSlot = new HashMap<>();

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        
        // Премиальный брендовый вывод в консоль для покупателя
        Bukkit.getConsoleSender().sendMessage("§d ");
        Bukkit.getConsoleSender().sendMessage("§d  ███████╗██╗  ██╗██╗   ██╗██╗     ██╗  ██╗███████╗██████╗ ");
        Bukkit.getConsoleSender().sendMessage("§d  ██╔════╝██║  ██║██║   ██║██║     ██║_ ██║██╔════╝██╔══██╗");
        Bukkit.getConsoleSender().sendMessage("§d  ███████╗███████║██║   ██║██║     █████_═╝█████╗  ██████╔╝");
        Bukkit.getConsoleSender().sendMessage("§d  ╚════██║██╔══██║██║   ██║██║     ██╔═██╗ ██╔══╝  ██╔══██╗");
        Bukkit.getConsoleSender().sendMessage("§d  ███████║██║  ██║╚██████╔╝███████╗██║  ██╗███████╗██║  ██║");
        Bukkit.getConsoleSender().sendMessage("§d  ╚══════╝╚═╝  ╚═╝ ╚═════╝ ╚══════╝╚═╝  ╚═╝╚══════╝╚═╝  ╚═╝");
        Bukkit.getConsoleSender().sendMessage("§5» [FrostShulkerAuto] Коммерческая лицензия активна! Продукт готов.");
        Bukkit.getConsoleSender().sendMessage("§5» [FrostShulkerAuto] Система умного авто-сбора лута запущена.\n");
    }

    // 1. Механика: Автоматический засос выбитых ресурсов прямо в шалкер на бегу
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        ItemStack itemToPickup = event.getItem().getItemStack();
        Inventory pInv = player.getInventory();

        // Если в инвентаре есть свободное место под этот предмет — пускай подбирается как обычно
        if (hasAvailableSlotFor(pInv, itemToPickup)) return;

        // Если инвентарь забит, ищем шалкер
        for (ItemStack clickedItem : pInv.getContents()) {
            if (clickedItem == null || !isShulkerBox(clickedItem.getType())) continue;

            BlockStateMeta bsm = (BlockStateMeta) clickedItem.getItemMeta();
            ShulkerBox shulker = (ShulkerBox) bsm.getBlockState();
            Inventory sInv = shulker.getInventory();

            // Проверяем, есть ли место внутри скрытого инвентаря шалкера
            if (hasAvailableSlotFor(sInv, itemToPickup)) {
                event.setCancelled(true); // Отменяем обычный подбор на землю

                // Добавляем вещь внутрь шалкера
                HashMap<Integer, ItemStack> leftOver = sInv.addItem(itemToPickup);
                bsm.setBlockState(shulker);
                clickedItem.setItemMeta(bsm);

                // Если предмет подобрался полностью
                if (leftOver.isEmpty()) {
                    event.getItem().remove();
                } else {
                    event.getItem().setItemStack(leftOver.get(0));
                }

                player.sendActionBar("§d§l[FROST-ШАЛКЕР] §f" + itemToPickup.getType().name() + " §aавтоматически убран внутрь!");
                player.playSound(player.getLocation(), Sound.BLOCK_SHULKER_BOX_CLOSE, 0.4f, 1.5f);
                break;
            }
        }
    }

    // 2. Механика: Просмотр и редактирование шалкера в инвентаре по клику ПКМ в воздухе
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || !isShulkerBox(item.getType())) return;
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            // Если игрок кликает по блоку шалкера, отменяем ванильную установку на землю
            if (event.getAction() == Action.RIGHT_CLICK_BLOCK) event.setCancelled(true);

            BlockStateMeta bsm = (BlockStateMeta) item.getItemMeta();
            ShulkerBox shulker = (ShulkerBox) bsm.getBlockState();

            String title = GUI_PREFIX + (bsm.hasDisplayName() ? bsm.getDisplayName() : "Шалкер");
            Inventory shulkerGui = Bukkit.createInventory(null, 27, title);
            shulkerGui.setContents(shulker.getInventory().getContents());

            // Запоминаем, в каком слоте лежит открытый шалкер, чтобы его не украли
            int slot = player.getInventory().getHeldItemSlot();
            openShulkerSlot.put(player.getUniqueId(), slot);

            player.openInventory(shulkerGui);
            player.playSound(player.getLocation(), Sound.BLOCK_SHULKER_BOX_OPEN, 0.6f, 1.0f);
        }
    }

    // Защита: Запрещаем игроку перетаскивать или выбрасывать сам шалкер, пока он открыт!
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().startsWith(GUI_PREFIX)) {
            Player player = (Player) event.getWhoClicked();
            int currentSlot = event.getSlot();
            int heldSlot = openShulkerSlot.getOrDefault(player.getUniqueId(), -1);

            if (currentSlot == heldSlot || (event.getHotbarButton() == heldSlot && heldSlot != -1)) {
                event.setCancelled(true);
            }
        }
    }

    // 3. Механика: Сохранение всех изменений в шалкер при закрытии инвентаря
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getView().getTitle().startsWith(GUI_PREFIX)) {
            Player player = (Player) event.getPlayer();
            UUID uuid = player.getUniqueId();

            int heldSlot = openShulkerSlot.remove(uuid);
            if (heldSlot == -1) return;

            ItemStack shulkerItem = player.getInventory().getItem(heldSlot);
            if (shulkerItem == null || !isShulkerBox(shulkerItem.getType())) return;

            BlockStateMeta bsm = (BlockStateMeta) shulkerItem.getItemMeta();
            ShulkerBox shulker = (ShulkerBox) bsm.getBlockState();

            // Записываем изменённое содержимое GUI обратно в NBT-теги предмета
            shulker.getInventory().setContents(event.getInventory().getContents());
            bsm.setBlockState(shulker);
            shulkerItem.setItemMeta(bsm);

            player.playSound(player.getLocation(), Sound.BLOCK_SHULKER_BOX_CLOSE, 0.6f, 1.1f);
        }
    }

    // Системный хелпер проверки свободного места
    private boolean hasAvailableSlotFor(Inventory inv, ItemStack item) {
        for (ItemStack is : inv.getStorageContents()) {
            if (is == null || is.getType() == Material.AIR) return true;
            if (is.isSimilar(item) && is.getAmount() < is.getMaxStackSize()) return true;
        }
        return false;
    }

    // Системный хелпер: является ли блок шалкером (все цвета)
    private boolean isShulkerBox(Material mat) {
        return mat.name().endsWith("SHULKER_BOX");
    }
}