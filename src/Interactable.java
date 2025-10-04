import java.awt.Rectangle;

interface Interactable {
    Rectangle getInteractionBounds();
    boolean isActive();
    String getInteractionPrompt();
    int getInteractionPriority();
    void interact(InteractionContext context);
    default void update(double dt) {
    }
}
