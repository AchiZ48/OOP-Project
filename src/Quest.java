class Quest {
    final String id;
    final String name;
    final String description;
    private QuestStatus status;
    private int rewardGold;

    Quest(String id, String name, String description) {
        this(id, name, description, 0);
    }

    Quest(String id, String name, String description, int rewardGold) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.rewardGold = Math.max(0, rewardGold);
        this.status = QuestStatus.AVAILABLE;
    }

    QuestStatus getStatus() {
        return status;
    }

    void setStatus(QuestStatus status) {
        this.status = status;
    }

    int getRewardGold() {
        return rewardGold;
    }

    void setRewardGold(int rewardGold) {
        this.rewardGold = Math.max(0, rewardGold);
    }

    boolean isComplete() {
        return status == QuestStatus.COMPLETED;
    }
}
