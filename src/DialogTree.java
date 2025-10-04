import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

class DialogTree {
    private final String id;
    private final Map<String, DialogNode> nodes = new HashMap<>();
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

    void addNode(DialogNode node) {
        if (node == null || node.id == null) {
            return;
        }
        nodes.put(node.id, node);
    }

    DialogNode getNode(String nodeId) {
        return nodes.get(nodeId);
    }

    Map<String, DialogNode> getNodes() {
        return Collections.unmodifiableMap(nodes);
    }
}
