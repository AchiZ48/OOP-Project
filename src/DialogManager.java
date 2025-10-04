import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class DialogManager {
    private DialogTree activeTree;
    private DialogNode currentNode;
    private final List<DialogChoice> availableChoices = new ArrayList<>();
    private InteractionContext context;
    private boolean textComplete = false;
    private boolean awaitingChoice = false;
    private double textTimer = 0.0;
    private double textSpeed = 40.0;
    private double speedMultiplier = 1.0;
    private int revealedCharacters = 0;
    private int selectedChoiceIndex = 0;

    void start(DialogTree tree, InteractionContext interactionContext) {
        if (tree == null) {
            return;
        }
        this.activeTree = tree;
        this.context = interactionContext;
        moveToNode(tree.getStartNodeId());
    }

    boolean isActive() {
        return activeTree != null && currentNode != null;
    }

    void update(double dt) {
        if (!isActive() || textComplete) {
            return;
        }
        textTimer += dt * textSpeed * speedMultiplier;
        int totalLength = currentNode.text != null ? currentNode.text.length() : 0;
        if (textTimer >= 1.0) {
            int advance = (int) textTimer;
            textTimer -= advance;
            revealedCharacters = Math.min(totalLength, revealedCharacters + advance);
            if (revealedCharacters >= totalLength) {
                textComplete = true;
                awaitingChoice = !availableChoices.isEmpty();
            }
        }
    }

    boolean isTextComplete() {
        return textComplete;
    }

    String getVisibleText() {
        if (!isActive() || currentNode.text == null) {
            return "";
        }
        if (textComplete) {
            return currentNode.text;
        }
        int endIndex = Math.min(currentNode.text.length(), revealedCharacters);
        return currentNode.text.substring(0, endIndex);
    }

    String getSpeaker() {
        return isActive() ? currentNode.speaker : null;
    }

    String getPortraitId() {
        return isActive() ? currentNode.portraitId : null;
    }

    List<DialogChoice> getAvailableChoices() {
        return Collections.unmodifiableList(availableChoices);
    }

    int getSelectedChoiceIndex() {
        return selectedChoiceIndex;
    }


    void setSpeedMultiplier(double multiplier) {
        speedMultiplier = Math.max(0.25, multiplier);
    }

    boolean isAwaitingChoice() {
        return awaitingChoice;
    }

    void moveSelection(int delta) {
        if (!awaitingChoice || availableChoices.isEmpty()) {
            return;
        }
        int size = availableChoices.size();
        selectedChoiceIndex = (selectedChoiceIndex + delta + size) % size;
    }

    void skipTextReveal() {
        if (!isActive()) {
            return;
        }
        revealedCharacters = currentNode.text != null ? currentNode.text.length() : 0;
        textComplete = true;
        awaitingChoice = !availableChoices.isEmpty();
    }

    void advance() {
        if (!isActive()) {
            return;
        }
        if (!textComplete) {
            skipTextReveal();
            return;
        }
        if (awaitingChoice && !availableChoices.isEmpty()) {
            return;
        }
        moveToNode(currentNode.nextNodeId);
    }

    void selectCurrentChoice() {
        selectChoice(selectedChoiceIndex);
    }

    void selectChoice(int index) {
        if (!awaitingChoice || index < 0 || index >= availableChoices.size()) {
            return;
        }
        DialogChoice choice = availableChoices.get(index);
        if (!choice.isAvailable(context)) {
            return;
        }
        selectedChoiceIndex = index;
        choice.execute(context);
        awaitingChoice = false;
        moveToNode(choice.nextNodeId);
    }

    void clear() {
        activeTree = null;
        currentNode = null;
        availableChoices.clear();
        context = null;
        textTimer = 0.0;
        revealedCharacters = 0;
        textComplete = false;
        awaitingChoice = false;
        selectedChoiceIndex = 0;
    }

    private void moveToNode(String nodeId) {
        if (activeTree == null || nodeId == null) {
            clear();
            return;
        }
        DialogNode node = activeTree.getNode(nodeId);
        if (node == null) {
            clear();
            return;
        }
        currentNode = node;
        availableChoices.clear();
        selectedChoiceIndex = 0;
        if (node.onEnter != null) {
            node.onEnter.execute(context);
        }
        if (!node.choices.isEmpty()) {
            for (DialogChoice choice : node.choices) {
                if (choice != null && choice.isAvailable(context)) {
                    availableChoices.add(choice);
                }
            }
        }
        textTimer = 0.0;
        revealedCharacters = 0;
        textComplete = node.text == null || node.text.isEmpty();
        awaitingChoice = textComplete && !availableChoices.isEmpty();
        if (textComplete && !awaitingChoice) {
            moveToNode(node.nextNodeId);
        }
    }
}
