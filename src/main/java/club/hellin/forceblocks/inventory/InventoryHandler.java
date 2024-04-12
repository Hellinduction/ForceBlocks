package club.hellin.forceblocks.inventory;

import org.bukkit.Material;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface InventoryHandler {
    Material type() default Material.STONE; // It is strongly recommended that you specify your own material, it is default stone to prevent forcing switcher handlers to unnecessarily include it
    String name(); // If switcher is true, name is not displayed, and is only used as a label to refer this item/handler
    String[] lore() default {};
    boolean enchanted() default true;
    boolean switcher() default false; // This means the items will change depending on a state
}