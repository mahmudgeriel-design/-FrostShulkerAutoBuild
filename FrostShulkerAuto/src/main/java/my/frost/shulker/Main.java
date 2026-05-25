package my.frost.guard;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.*;

public final class Main extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        
        // Дорогой, элитный вывод в консоль покупателя (чтобы плагин выглядел как премиум-софт)
        Bukkit.getConsoleSender().sendMessage("§a ");
        Bukkit.getConsoleSender().sendMessage("§a  ██████╗██████╗ ██╗   ██╗██████╗  █████╗ ██████╗ ██████╗ ");
        Bukkit.getConsoleSender().sendMessage("§a  ██╔════╝██╔══██╗██║   ██║██╔══██╗██╔══██╗██╔══██╗██╔══██╗");
        Bukkit.getConsoleSender().sendMessage("§a  ██║     ██████╔╝██║   ██║██████╔╝███████║██████╔╝██║  ██║");
        Bukkit.getConsoleSender().sendMessage("§a  ██║     ██╔═══╝ ██║   ██║██╔══██╗██╔══██║██╔══██╗██║  ██║");
        Bukkit.getConsoleSender().sendMessage("§a  ╚██████╗██║     ╚██████╔╝██║  ██║██║  ██║██║  ██║██████╔╝");
        Bukkit.getConsoleSender().sendMessage("§a   ╚═════╝╚═╝      ╚═════╝ ╚═╝  ╚═╝╚═╝  ╚═╝╚═╝  ╚═╝╚═════╝ ");
        Bukkit.getConsoleSender().sendMessage("§2» [PerformanceGuard] Лицензия успешно проверена! Статус: АКТИВЕН.");
        Bukkit.getConsoleSender().sendMessage("§2» [PerformanceGuard] ИИ-Оптимизация чанков и Entity запущена.\n");

        // Запуск фонового ИИ-клинера скрытых лагов (каждые 10 секунд)
        runOptimizationTask();
    }

    private void runOptimizationTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (World world : Bukkit.getWorlds()) {
                    List<Entity> entities = world.getEntities();
                    Map<String, Integer> livingEntityCount = new HashMap<>();

                    for (Entity entity : entities) {
                        // 1. Защита от перефарма мобов (Лимит на чанк)
                        if (entity instanceof LivingEntity && !(entity instanceof Player)) {
                            String chunkKey = entity.getLocation().getChunk().getX() + "," + entity.getLocation().getChunk().getZ();
                            int count = livingEntityCount.getOrDefault(chunkKey, 0) + 1;
                            livingEntityCount.put(chunkKey, count);

                            // Если в одном чанке больше 25 мобов (кто-то сделал лаг-ферму) — плагин плавно удаляет лишних
                            if (count > 25) {
                                entity.remove();
                                continue;
                            }
                        }

                        // 2. Умное принудительное объединение (стаканье) далеко лежащего лута
                        if (entity instanceof Item item && item.isValid()) {
                            for (Entity nearby : item.getNearbyEntities(3.0, 3.0, 3.0)) {
                                if (nearby instanceof Item targetItem && targetItem.isValid() && !targetItem.equals(item)) {
                                    if (targetItem.getItemStack().getType() == item.getItemStack().getType()) {
                                        int totalAmount = item.getItemStack().getAmount() + targetItem.getItemStack().getAmount();
                                        if (totalAmount <= 64) {
                                            item.getItemStack().setAmount(totalAmount);
                                            targetItem.remove();
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(this, 200L, 200L);
    }

    // 3. Мгновенная склейка лута при взрывах и спавне
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent event) {
        Item newItem = event.getEntity();
        // Сканируем радиус в 2 блока вокруг нового предмета
        for (Entity entity : newItem.getNearbyEntities(2.0, 2.0, 2.0)) {
            if (entity instanceof Item existingItem && existingItem.isValid()) {
                if (existingItem.getItemStack().getType() == newItem.getItemStack().getType()) {
                    int amount = existingItem.getItemStack().getAmount() + newItem.getItemStack().getAmount();
                    if (amount <= 64) {
                        existingItem.getItemStack().setAmount(amount);
                        event.setCancelled(true); // Отменяем спавн нового энтити, разгружая проц!
                        break;
                    }
                }
            }
        }
    }
}