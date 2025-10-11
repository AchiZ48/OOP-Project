class MedicNPC extends NPC {
    private static final int WIDTH = 32;
    private static final int HEIGHT = 48;
    private final int freeHealsPerRest;
    private final int premiumCost;
    private final int supplyCost = 20;
    private final int essenceBonus = 2;
    private final String questId = "quest_medic_supplies";
    private int freeHealsRemaining;

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
                "medic_pt",
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
                        new DialogChoice("I can fund your supplies.", "quest_accept",
                                ctx -> offerQuest(ctx),
                                ctx -> canOfferQuest(ctx)),
                        new DialogChoice("I brought the gold you need.", "quest_complete",
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
                "Bring me 40 gold so I can restock proper supplies.",
                "medic",
                "exit",
                java.util.Collections.emptyList(),
                null
        );
        tree.addNode(questAccept);

        DialogNode questComplete = new DialogNode(
                "quest_complete",
                name,
                "Marvelous work! The party will appreciate the fresh supplies.",
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
                && context.getGold() >= supplyCost;
    }

    private void offerQuest(InteractionContext context) {
        QuestManager manager = context.getQuestManager();
        if (manager == null) {
            return;
        }
        Quest quest = manager.getQuest(questId);
        if (quest == null) {
            quest = new Quest(questId, "Medic Supplies", "Contribute 40 gold to restock the medic's satchel.", 60);
            manager.registerQuest(quest);
        }
        quest.setStatus(QuestStatus.ACTIVE);
        context.queueMessage("Quest accepted: Medic Supplies");
        context.queueMessage("Bring " + supplyCost + " gold back to Selene.");
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
        if (!context.spendGold(supplyCost)) {
            context.queueMessage("You still need more gold for the supplies.");
            return;
        }
        quest.setStatus(QuestStatus.COMPLETED);
        int reward = quest.getRewardGold();
        context.addGold(reward);
        if (essenceBonus > 0) {
            context.addEssence(essenceBonus);
        }
        context.unlockFastTravel("ruins");
        context.queueMessage("Ruins waypoint is now attuned.");
        context.queueMessage("Received " + reward + " gold for helping the medic.");
        if (essenceBonus > 0) {
            context.queueMessage("Gained " + essenceBonus + " essence for your generosity.");
        }
    }

    private String questText(InteractionContext context) {
        QuestManager manager = context.getQuestManager();
        if (manager == null) {
            return "I am running low on supplies, but I suppose that is not your concern.";
        }
        Quest quest = manager.getQuest(questId);
        if (quest == null || quest.getStatus() == QuestStatus.AVAILABLE) {
            return "If you can spare " + supplyCost + " gold, I could restock my supplies.";
        }
        if (quest.getStatus() == QuestStatus.ACTIVE) {
            return "Have you gathered the " + supplyCost + " gold yet?";
        }
        if (quest.getStatus() == QuestStatus.COMPLETED) {
            return "Thanks again for covering the supply costs. The tinctures are flowing.";
        }
        return "Stay healthy out there.";
    }

    void resetDailySupplies() {
        freeHealsRemaining = freeHealsPerRest;
    }
}
