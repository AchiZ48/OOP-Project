import java.awt.*;

class MedicNPC extends NPC {
    private static final int WIDTH = 32;
    private static final int HEIGHT = 48;
    private final int freeHealsPerRest;
    private int freeHealsRemaining;
    private final int premiumCost;
    private final String questId = "quest_medic_herbs";

    MedicNPC(String id, String name, Sprite sprite, double x, double y) {
        super(id, name, sprite, x, y, WIDTH, HEIGHT);
        this.freeHealsPerRest = 1;
        this.freeHealsRemaining = freeHealsPerRest;
        this.premiumCost = 35;
        setPrompt("Speak with Medic");
        setInteractionPriority(4);

        Stats stats = Stats.createDefault();
        stats.setBaseValue(Stats.StatType.MAX_HP, 60);
        stats.setBaseValue(Stats.StatType.ARCANE, 8);
        stats.setBaseValue(Stats.StatType.AWARENESS, 6);
        stats.setBaseValue(Stats.StatType.LUCK, 5);
        stats.fullHeal();
        stats.fullRestoreBattlePoints();
//        setStats(stats);
    }

    @Override
    public void update(double dt) {
        if (sprite != null) {
            sprite.update(dt);
        }
    }

    @Override
    protected void onInteract(InteractionContext context) {
        DialogTree tree = buildDialogTree(context);
        context.startDialog(tree);
    }

    private DialogTree buildDialogTree(InteractionContext context) {
        DialogTree tree = new DialogTree("medic_root", "intro");

        DialogNode intro = new DialogNode(
                "intro",
                name,
                "You look weary. Need patching up or perhaps something stronger?",
                "medic",
                null,
                java.util.List.of(
                        new DialogChoice(freeHealLabel(), "free_heal",
                                ctx -> performFreeHeal(ctx),
                                ctx -> freeHealsRemaining > 0),
                        new DialogChoice("Premium treatment (" + premiumCost + " gold)", "premium_heal",
                                ctx -> performPremiumHeal(ctx),
                                ctx -> ctx.getGold() >= premiumCost),
                        new DialogChoice("Any work available?", "quest_branch"),
                        new DialogChoice("Stay healthy!", "exit")
                ),
                null
        );
        tree.addNode(intro);

        DialogNode freeHeal = new DialogNode(
                "free_heal",
                name,
                "Take it easy. That should hold for now.",
                "medic",
                "exit",
                java.util.Collections.emptyList(),
                null
        );
        tree.addNode(freeHeal);

        DialogNode premiumHeal = new DialogNode(
                "premium_heal",
                name,
                "Full restoration applied. Try not to bleed on my floor again.",
                "medic",
                "exit",
                java.util.Collections.emptyList(),
                null
        );
        tree.addNode(premiumHeal);

        DialogNode questBranch = new DialogNode(
                "quest_branch",
                name,
                questText(context),
                "medic",
                null,
                java.util.List.of(
                        new DialogChoice("I can gather the herbs.", "quest_accept",
                                ctx -> offerQuest(ctx),
                                ctx -> canOfferQuest(ctx)),
                        new DialogChoice("I found the herbs you wanted.", "quest_complete",
                                ctx -> completeQuest(ctx),
                                ctx -> canCompleteQuest(ctx)),
                        new DialogChoice("Maybe later.", "exit")
                ),
                null
        );
        tree.addNode(questBranch);

        DialogNode questAccept = new DialogNode(
                "quest_accept",
                name,
                "Bring me three sprigs of dawnblossom from the forest glade. They glow faintly at night.",
                "medic",
                "exit",
                java.util.Collections.emptyList(),
                null
        );
        tree.addNode(questAccept);

        DialogNode questComplete = new DialogNode(
                "quest_complete",
                name,
                "Marvelous work! The party will appreciate a proper tonic.",
                "medic",
                "exit",
                java.util.Collections.emptyList(),
                null
        );
        tree.addNode(questComplete);

        DialogNode exit = new DialogNode(
                "exit",
                name,
                "Stay safe out there.",
                "medic",
                null,
                java.util.Collections.emptyList(),
                null
        );
        tree.addNode(exit);
        return tree;
    }

    private String freeHealLabel() {
        return freeHealsRemaining > 0 ? "Quick patch-up (free)" : "Quick patch-up (unavailable)";
    }

    private void performFreeHeal(InteractionContext context) {
        if (freeHealsRemaining <= 0) {
            context.queueMessage("Medic is out of free supplies for now.");
            return;
        }
        context.getGamePanel().healPartyPercentage(0.5);
        context.playSfx("medic_heal");
        freeHealsRemaining--;
    }

    private void performPremiumHeal(InteractionContext context) {
        if (!context.spendGold(premiumCost)) {
            context.queueMessage("Not enough gold for premium treatment.");
            return;
        }
        context.getGamePanel().healPartyFull();
        context.playSfx("medic_heal");
        context.queueMessage("The party feels revitalized!");
    }

    private boolean canOfferQuest(InteractionContext context) {
        QuestManager manager = context.getQuestManager();
        if (manager == null) {
            return false;
        }
        Quest quest = manager.getQuest(questId);
        return quest == null || quest.getStatus() == QuestStatus.AVAILABLE;
    }

    private boolean canCompleteQuest(InteractionContext context) {
        QuestManager manager = context.getQuestManager();
        if (manager == null) {
            return false;
        }
        Quest quest = manager.getQuest(questId);
        return quest != null && quest.getStatus() == QuestStatus.ACTIVE
                && context.getGamePanel().hasInventoryItem("herb_dawnblossom", 3);
    }

    private void offerQuest(InteractionContext context) {
        QuestManager manager = context.getQuestManager();
        if (manager == null) {
            return;
        }
        Quest quest = manager.getQuest(questId);
        if (quest == null) {
            quest = new Quest(questId, "Medic Supplies", "Gather three dawnblossom herbs for the medic.", 60);
            manager.registerQuest(quest);
        }
        quest.setStatus(QuestStatus.ACTIVE);
        context.queueMessage("Quest accepted: Medic Supplies");
        context.getGamePanel().markQuestTarget("herb_dawnblossom");
    }

    private void completeQuest(InteractionContext context) {
        QuestManager manager = context.getQuestManager();
        if (manager == null) {
            return;
        }
        Quest quest = manager.getQuest(questId);
        if (quest == null) {
            return;
        }
        quest.setStatus(QuestStatus.COMPLETED);
        context.getGamePanel().consumeInventoryItem("herb_dawnblossom", 3);
        int reward = quest.getRewardGold();
        context.addGold(reward);
        context.unlockFastTravel("ruins");
        context.queueMessage("Ruins waypoint is now attuned.");
        context.queueMessage("Received " + reward + " gold for helping the medic.");
    }

    private String questText(InteractionContext context) {
        QuestManager manager = context.getQuestManager();
        if (manager == null) {
            return "I am running low on herbs, but I suppose that is not your concern.";
        }
        Quest quest = manager.getQuest(questId);
        if (quest == null || quest.getStatus() == QuestStatus.AVAILABLE) {
            return "If you have a moment, I could use a few rare herbs from the glade.";
        }
        if (quest.getStatus() == QuestStatus.ACTIVE) {
            return "Have you gathered the dawnblossom sprigs yet?";
        }
        if (quest.getStatus() == QuestStatus.COMPLETED) {
            return "Thanks again for the herbs. The tinctures are working wonders.";
        }
        return "Stay healthy out there.";
    }

    void resetDailySupplies() {
        freeHealsRemaining = freeHealsPerRest;
    }
}

