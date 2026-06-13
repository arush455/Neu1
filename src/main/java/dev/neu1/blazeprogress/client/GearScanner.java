package dev.neu1.blazeprogress.client;

import dev.neu1.blazeprogress.core.GearSnapshot;
import dev.neu1.blazeprogress.core.ItemSnapshot;
import dev.neu1.blazeprogress.core.KuudraTier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

import java.util.Set;

/**
 * Reads the player's worn armour and inventory and turns SkyBlock items into
 * {@link ItemSnapshot}s.
 *
 * ── NBT API (targeting 1.21.11 / 1.21.5+) ────────────────────────────────────
 *  On 1.21.5+ the NbtCompound getters return {@link java.util.Optional}. All NBT
 *  access is funnelled through {@link #extraAttributes} and the {@code str()/
 *  integer()} helpers below, which use that Optional API. If you ever build for
 *  1.21.4 or earlier instead, revert those three methods to the boolean
 *  {@code contains()} + direct-return form.
 */
public final class GearScanner {

    /** SkyBlock id roots whose tier we infer from the display name. */
    private static final Set<String> KUUDRA_FAMILY = Set.of(
            "CRIMSON_", "TERROR_", "AURORA_", "HOLLOW_", "FERVOR_");

    private GearScanner() {}

    public static GearSnapshot scan(MinecraftClient mc) {
        GearSnapshot snap = new GearSnapshot();
        ClientPlayerEntity player = mc.player;
        if (player == null) return snap;

        snap.helmet = parse(player.getEquippedStack(EquipmentSlot.HEAD));
        snap.chestplate = parse(player.getEquippedStack(EquipmentSlot.CHEST));
        snap.leggings = parse(player.getEquippedStack(EquipmentSlot.LEGS));
        snap.boots = parse(player.getEquippedStack(EquipmentSlot.FEET));

        PlayerInventory inv = player.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            ItemSnapshot it = parse(inv.getStack(i));
            if (!it.isEmpty()) snap.inventory.add(it);
        }
        return snap;
    }

    public static ItemSnapshot parse(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return ItemSnapshot.EMPTY;

        NbtCompound ea = extraAttributes(stack);
        if (ea == null) return ItemSnapshot.EMPTY;

        String id = str(ea, "id");
        if (id == null) return ItemSnapshot.EMPTY;

        String reforge = str(ea, "modifier");
        int stars = Math.max(integer(ea, "upgrade_level"), integer(ea, "dungeon_item_level"));
        String name = stack.getName().getString();

        KuudraTier tier = isKuudraFamily(id) ? KuudraTier.fromDisplayName(name) : null;
        return new ItemSnapshot(id, reforge, stars, tier, name);
    }

    private static boolean isKuudraFamily(String id) {
        for (String root : KUUDRA_FAMILY) {
            if (id.startsWith(root)) return true;
        }
        return false;
    }

    /** Returns the {@code ExtraAttributes} compound, or null if absent. */
    static NbtCompound extraAttributes(ItemStack stack) {
        NbtComponent comp = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (comp == null) return null;
        NbtCompound nbt = comp.copyNbt();
        // 1.21.5+: getCompound returns Optional<NbtCompound>.
        return nbt.getCompound("ExtraAttributes").orElse(null);
    }

    // ── NBT primitive helpers (1.21.5+ Optional API) ─────────────────────────
    static String str(NbtCompound nbt, String key) {
        return nbt.getString(key).orElse(null);
    }

    static int integer(NbtCompound nbt, String key) {
        return nbt.getInt(key).orElse(0);
    }
}
