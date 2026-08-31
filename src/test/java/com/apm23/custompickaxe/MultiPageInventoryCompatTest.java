package com.apm23.custompickaxe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

final class MultiPageInventoryCompatTest {
    @Test
    void fillsExistingStacksBeforeEmptyHiddenSlotsWithoutLoss() {
        ArrayList<ItemStack> page = emptyPage();
        page.set(0, new ItemStack(Items.DIAMOND, 60));
        ItemStack remaining = new ItemStack(Items.DIAMOND, 70);

        MultiPageInventoryCompat.insertInto(page, remaining);

        assertTrue(remaining.isEmpty());
        assertEquals(64, page.get(0).getCount());
        assertEquals(64, page.get(1).getCount());
        assertEquals(2, page.get(2).getCount());
        assertEquals(130, count(page, Items.DIAMOND));
    }

    @Test
    void fullPageLeavesExactRemainderForFollowingPages() {
        ArrayList<ItemStack> page = new ArrayList<>(27);
        for (int i = 0; i < 27; i++) page.add(new ItemStack(Items.STONE, 64));
        ItemStack remaining = new ItemStack(Items.DIAMOND, 37);

        MultiPageInventoryCompat.insertInto(page, remaining);

        assertEquals(37, remaining.getCount());
        assertEquals(0, count(page, Items.DIAMOND));
    }

    @Test
    void differentItemsAreNeverMerged() {
        ArrayList<ItemStack> page = emptyPage();
        page.set(0, new ItemStack(Items.EMERALD, 10));
        ItemStack remaining = new ItemStack(Items.DIAMOND, 5);

        MultiPageInventoryCompat.insertInto(page, remaining);

        assertTrue(remaining.isEmpty());
        assertEquals(10, page.get(0).getCount());
        assertEquals(5, page.get(1).getCount());
    }

    private static ArrayList<ItemStack> emptyPage() {
        ArrayList<ItemStack> page = new ArrayList<>(27);
        for (int i = 0; i < 27; i++) page.add(ItemStack.EMPTY);
        return page;
    }

    private static int count(List<ItemStack> stacks, net.minecraft.world.item.Item item) {
        return stacks.stream().filter(stack -> !stack.isEmpty() && stack.is(item)).mapToInt(ItemStack::getCount).sum();
    }
}
