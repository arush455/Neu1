package dev.neu1.blazeprogress.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.neu1.blazeprogress.core.PetSnapshot;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The active pet can't be read from the inventory, so we sniff it from the
 * SkyBlock "Pets" menu when it is open. The last detected pet is cached.
 */
public final class PetTracker {

    private static final Pattern LEVEL = Pattern.compile("\\[Lvl (\\d+)]");

    private static volatile PetSnapshot current; // null until first scan

    private PetTracker() {}

    public static PetSnapshot current() {
        return current;
    }

    public static boolean hasScanned() {
        return current != null;
    }

    /** Call every client tick; cheap no-op unless a Pets menu is open. */
    public static void tick(MinecraftClient mc) {
        Screen screen = mc.currentScreen;
        if (!(screen instanceof HandledScreen<?> handled)) return;

        String title = screen.getTitle() == null ? "" : screen.getTitle().getString();
        if (!title.contains("Pets")) return;

        for (Slot slot : handled.getScreenHandler().slots) {
            ItemStack stack = slot.getStack();
            if (stack == null || stack.isEmpty()) continue;
            if (isActivePet(stack)) {
                current = parsePet(stack);
                return;
            }
        }
    }

    private static boolean isActivePet(ItemStack stack) {
        LoreComponent lore = stack.get(DataComponentTypes.LORE);
        if (lore == null) return false;
        for (Text line : lore.lines()) {
            if (line.getString().toLowerCase().contains("click to despawn")) return true;
        }
        return false;
    }

    private static PetSnapshot parsePet(ItemStack stack) {
        String type = null;
        String tier = null;

        NbtCompound ea = GearScanner.extraAttributes(stack);
        String petInfo = ea == null ? null : GearScanner.str(ea, "petInfo");
        if (petInfo != null) {
            try {
                JsonObject o = JsonParser.parseString(petInfo).getAsJsonObject();
                if (o.has("type")) type = o.get("type").getAsString();
                if (o.has("tier")) tier = o.get("tier").getAsString();
            } catch (Exception ignored) {
                // Fall through to name-based parsing.
            }
        }

        String name = stack.getName().getString();
        int level = 0;
        Matcher m = LEVEL.matcher(name);
        if (m.find()) level = Integer.parseInt(m.group(1));

        return new PetSnapshot(type, tier, level, name);
    }
}
