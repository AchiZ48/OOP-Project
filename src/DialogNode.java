import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class DialogNode {
    final String id;
    final String speaker;
    final String text;
    final String portraitId;
    final String nextNodeId;
    final List<DialogChoice> choices;
    final DialogAction onEnter;

    DialogNode(String id,
               String speaker,
               String text,
               String portraitId,
               String nextNodeId,
               List<DialogChoice> choices,
               DialogAction onEnter) {
        this.id = id;
        this.speaker = speaker;
        this.text = text;
        this.portraitId = portraitId;
        this.nextNodeId = nextNodeId;
        this.onEnter = onEnter;
        this.choices = choices != null ? new ArrayList<>(choices) : new ArrayList<>();
    }

    List<DialogChoice> getChoices() {
        return Collections.unmodifiableList(choices);
    }

    boolean hasChoices() {
        return !choices.isEmpty();
    }
}
