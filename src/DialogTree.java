import java.util.*;

class DialogTree {
    private final String id;
    private final Map<String, Node> nodes = new HashMap<>();
    private final String startNodeId;

    DialogTree(String id, String startNodeId) {
        this.id = id;
        this.startNodeId = startNodeId;
    }

    String getId() {
        return id;
    }

    String getStartNodeId() {
        return startNodeId;
    }

    void addNode(Node node) {
        if (node == null || node.id == null) {
            return;
        }
        nodes.put(node.id, node);
    }

    Node getNode(String nodeId) {
        return nodes.get(nodeId);
    }

    Map<String, Node> getNodes() {
        return Collections.unmodifiableMap(nodes);
    }

    interface Action {
        void execute(WorldObjectManager.InteractionContext context);
    }

    interface Condition {
        boolean test(WorldObjectManager.InteractionContext context);
    }

    static final class Choice {
        final String text;
        final String nextNodeId;
        final Action action;
        final Condition condition;

        Choice(String text, String nextNodeId) {
            this(text, nextNodeId, null, null);
        }

        Choice(String text, String nextNodeId, Action action, Condition condition) {
            this.text = text;
            this.nextNodeId = nextNodeId;
            this.action = action;
            this.condition = condition;
        }

        boolean isAvailable(WorldObjectManager.InteractionContext context) {
            return condition == null || condition.test(context);
        }

        void execute(WorldObjectManager.InteractionContext context) {
            if (action != null) {
                action.execute(context);
            }
        }
    }

    static final class Node {
        final String id;
        final String speaker;
        final String text;
        final String portraitId;
        final String nextNodeId;
        final List<Choice> choices;
        final Action onEnter;

        Node(String id,
             String speaker,
             String text,
             String portraitId,
             String nextNodeId,
             List<Choice> choices,
             Action onEnter) {
            this.id = id;
            this.speaker = speaker;
            this.text = text;
            this.portraitId = portraitId;
            this.nextNodeId = nextNodeId;
            this.onEnter = onEnter;
            this.choices = choices != null ? new ArrayList<>(choices) : new ArrayList<>();
        }

        List<Choice> getChoices() {
            return Collections.unmodifiableList(choices);
        }

        boolean hasChoices() {
            return !choices.isEmpty();
        }
    }
}
