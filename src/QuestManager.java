import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

class QuestManager {
    private final Map<String, Quest> quests = new HashMap<>();

    void registerQuest(Quest quest) {
        if (quest == null || quest.id == null) {
            return;
        }
        quests.put(quest.id, quest);
    }

    Quest getQuest(String id) {
        return quests.get(id);
    }

    void setQuestStatus(String id, QuestStatus status) {
        Quest quest = quests.get(id);
        if (quest != null) {
            quest.setStatus(status);
        }
    }

    boolean isQuestComplete(String id) {
        Quest quest = quests.get(id);
        return quest != null && quest.isComplete();
    }

    Collection<Quest> getQuests() {
        return Collections.unmodifiableCollection(quests.values());
    }
}
