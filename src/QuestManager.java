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

    void setQuestStatus(String id, Status status) {
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

    void clear() {
        quests.clear();
    }

    enum Status {
        AVAILABLE,
        ACTIVE,
        COMPLETED,
        FAILED
    }

    static final class Quest {
        final String id;
        final String name;
        final String description;
        private Status status;
        private int rewardGold;

        Quest(String id, String name, String description) {
            this(id, name, description, 0);
        }

        Quest(String id, String name, String description, int rewardGold) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.rewardGold = Math.max(0, rewardGold);
            this.status = Status.AVAILABLE;
        }

        Status getStatus() {
            return status;
        }

        void setStatus(Status status) {
            this.status = status;
        }

        int getRewardGold() {
            return rewardGold;
        }

        void setRewardGold(int rewardGold) {
            this.rewardGold = Math.max(0, rewardGold);
        }

        boolean isComplete() {
            return status == Status.COMPLETED;
        }
    }
}
