package com.apm23.custompickaxe;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Optional compatibility with apm23/custom-hotbar-inventory.
 *
 * The inventory mod is intentionally not a hard dependency. When it is present, overflow from the
 * live vanilla inventory is routed through every hidden inventory page and the alternate hotbar
 * before callers fall back to dropping the remainder into the world.
 */
final class MultiPageInventoryCompat {
    private static final String STORAGE_CLASS = "com.anjas.custominventory.InventoryStorage";
    private static volatile boolean resolved;
    private static volatile Access access;

    private MultiPageInventoryCompat() {}

    static void insertOverflow(ServerPlayer player, ItemStack remaining) {
        if (remaining.isEmpty()) return;

        Access api = resolve();
        if (api == null) return;

        try {
            api.snapshotLive.invoke(null, player);
            int activePage = (Integer) api.active.invoke(null, player);

            for (int page = 0; page < api.pageCount && !remaining.isEmpty(); page++) {
                if (page == activePage) continue;
                List<ItemStack> slots = mutableCopy(api.read.invoke(null, player, page), api.pageSize);
                insertInto(slots, remaining);
                api.write.invoke(null, player, page, slots);
            }

            if (!remaining.isEmpty()) {
                List<ItemStack> hotbar = mutableCopy(api.readAltHotbar.invoke(null, player), 9);
                insertInto(hotbar, remaining);
                api.writeAltHotbar.invoke(null, player, hotbar);
            }

            api.sync.invoke(null, player);
        } catch (ReflectiveOperationException | RuntimeException error) {
            CustomPickaxeMod.LOGGER.warn("Could not route mining overflow into multi-page inventory; falling back to world drop", error);
        }
    }

    static void insertInto(List<ItemStack> slots, ItemStack remaining) {
        if (remaining.isEmpty()) return;

        for (ItemStack existing : slots) {
            if (remaining.isEmpty()) return;
            if (existing == null || existing.isEmpty() || !ItemStack.isSameItemSameComponents(existing, remaining)) continue;
            int room = existing.getMaxStackSize() - existing.getCount();
            if (room <= 0) continue;
            int moved = Math.min(room, remaining.getCount());
            existing.grow(moved);
            remaining.shrink(moved);
        }

        for (int slot = 0; slot < slots.size() && !remaining.isEmpty(); slot++) {
            ItemStack existing = slots.get(slot);
            if (existing != null && !existing.isEmpty()) continue;
            int moved = Math.min(remaining.getCount(), remaining.getMaxStackSize());
            ItemStack placed = remaining.copy();
            placed.setCount(moved);
            slots.set(slot, placed);
            remaining.shrink(moved);
        }
    }

    @SuppressWarnings("unchecked")
    private static List<ItemStack> mutableCopy(Object raw, int size) {
        List<ItemStack> source = raw instanceof List<?> list ? (List<ItemStack>) list : List.of();
        ArrayList<ItemStack> out = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            ItemStack stack = i < source.size() && source.get(i) != null ? source.get(i) : ItemStack.EMPTY;
            out.add(stack.copy());
        }
        return out;
    }

    private static Access resolve() {
        if (resolved) return access;
        synchronized (MultiPageInventoryCompat.class) {
            if (resolved) return access;
            try {
                Class<?> type = Class.forName(STORAGE_CLASS);
                access = new Access(
                        type.getField("PAGE_COUNT").getInt(null),
                        type.getField("PAGE_SIZE").getInt(null),
                        type.getMethod("snapshotLive", ServerPlayer.class),
                        type.getMethod("active", ServerPlayer.class),
                        type.getMethod("read", ServerPlayer.class, int.class),
                        type.getMethod("write", ServerPlayer.class, int.class, List.class),
                        type.getMethod("readAltHotbar", ServerPlayer.class),
                        type.getMethod("writeAltHotbar", ServerPlayer.class, List.class),
                        type.getMethod("sync", ServerPlayer.class));
                CustomPickaxeMod.LOGGER.info("Multi-page inventory compatibility enabled");
            } catch (ClassNotFoundException ignored) {
                access = null;
            } catch (ReflectiveOperationException error) {
                CustomPickaxeMod.LOGGER.warn("Multi-page inventory detected but compatibility API could not be resolved", error);
                access = null;
            }
            resolved = true;
            return access;
        }
    }

    private record Access(
            int pageCount,
            int pageSize,
            Method snapshotLive,
            Method active,
            Method read,
            Method write,
            Method readAltHotbar,
            Method writeAltHotbar,
            Method sync) {}
}
