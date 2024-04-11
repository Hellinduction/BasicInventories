package club.hellin.util.basicinventories;

import org.bukkit.Material;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface InventoryHandler {
    Material type();
    String name();
    String[] lore() default {};
    boolean enchanted() default true;
    boolean switcher() default false;
}