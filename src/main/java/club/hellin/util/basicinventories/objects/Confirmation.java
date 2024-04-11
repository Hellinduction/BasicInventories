package club.hellin.util.basicinventories.objects;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public final class Confirmation {
    public enum Status {
        CONFIRMED,
        DENIED;

        public boolean isSuccessful() {
            return this == CONFIRMED;
        }
    }

    private final Status status;
    private final InventoryClick click;
}