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
    protected void onInteract(WorldObjectManager.InteractionContext context) {
        DialogTree tree = buildDialogTree(context);
        context.startDialog(tree);
    }

    private DialogTree buildDialogTree(WorldObjectManager.InteractionContext context) {
        DialogTree tree = new DialogTree("medic_root", "intro");

        DialogTree.Node intro = new DialogTree.Node(
                "intro",
                name,
                "You look weary. Need patching up or perhaps something stronger?",
                "medic_pt",
                null,
                java.util.List.of(
                        new DialogTree.Choice(freeHealLabel(), "free_heal",
                                ctx -> performFreeHeal(ctx),
                                ctx -> freeHealsRemaining > 0),
                        new DialogTree.Choice("Premium treatment (" + premiumCost + " gold)", "premium_heal",
                                ctx -> performPremiumHeal(ctx),
                                ctx -> ctx.getGold() >= premiumCost),
                        new DialogTree.Choice("Any work available?", "quest_branch"),
                        new DialogTree.Choice("Stay healthy!", "exit")
                ),
                null
        );
        tree.addNode(intro);

        DialogTree.Node freeHeal = new DialogTree.Node(
                "free_heal",
                name,
                "Take it easy. That should hold for now.",
                "medic_pt",
                "exit",
                java.util.Collections.emptyList(),
                null
        );
        tree.addNode(freeHeal);

        DialogTree.Node premiumHeal = new DialogTree.Node(
                "premium_heal",
                name,
                "Full restoration applied. Try not to bleed on my floor again.",
                "medic_pt",
                "exit",
                java.util.Collections.emptyList(),
                null
        );
        tree.addNode(premiumHeal);

        DialogTree.Node questBranch = new DialogTree.Node(
                "quest_branch",
                name,
                questText(context),
                "medic_pt",
                null,
                java.util.List.of(
                        new DialogTree.Choice("I can fund your supplies.", "quest_accept",
                                ctx -> offerQuest(ctx),
                                ctx -> canOfferQuest(ctx)),
                        new DialogTree.Choice("I brought the gold you need.", "quest_complete",
                                ctx -> completeQuest(ctx),
                                ctx -> canCompleteQuest(ctx)),
                        new DialogTree.Choice("Maybe later.", "exit")
                ),
                null
        );
        tree.addNode(questBranch);

        DialogTree.Node questAccept = new DialogTree.Node(
                "quest_accept",
                name,
                "Bring me 40 gold so I can restock proper supplies.",
                "medic_pt",
                "exit",
                java.util.Collections.emptyList(),
                null
        );
        tree.addNode(questAccept);

        DialogTree.Node questComplete = new DialogTree.Node(
                "quest_complete",
                name,
                "Marvelous work! The party will appreciate the fresh supplies.",
                "medic_pt",
                "exit",
                java.util.Collections.emptyList(),
                null
        );
        tree.addNode(questComplete);

        DialogTree.Node exit = new DialogTree.Node(
                "exit",
                name,
                "Stay safe out there.",
                "medic_pt",
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

    private void performFreeHeal(WorldObjectManager.InteractionContext context) {
        if (freeHealsRemaining <= 0) {
            context.queueMessage("Medic is out of free supplies for now.");
            return;
        }
        context.getGamePanel().healPartyPercentage(0.5);
        context.playSfx("medic_heal");
        freeHealsRemaining--;
    }

    private void performPremiumHeal(WorldObjectManager.InteractionContext context) {
        if (!context.spendGold(premiumCost)) {
            context.queueMessage("Not enough gold for premium treatment.");
            return;
        }
        context.getGamePanel().healPartyFull();
        context.playSfx("medic_heal");
        context.queueMessage("The party feels revitalized!");
    }

    private boolean canOfferQuest(WorldObjectManager.InteractionContext context) {
        QuestManager manager = context.getQuestManager();
        if (manager == null) {
            return false;
        }
        QuestManager.Quest quest = manager.getQuest(questId);
        return quest == null || quest.getStatus() == QuestManager.Status.AVAILABLE;
    }

    private boolean canCompleteQuest(WorldObjectManager.InteractionContext context) {
        QuestManager manager = context.getQuestManager();
        if (manager == null) {
            return false;
        }
        QuestManager.Quest quest = manager.getQuest(questId);
        return quest != null && quest.getStatus() == QuestManager.Status.ACTIVE
                && context.getGold() >= supplyCost;
    }

    private void offerQuest(WorldObjectManager.InteractionContext context) {
        QuestManager manager = context.getQuestManager();
        if (manager == null) {
            return;
        }
        QuestManager.Quest quest = manager.getQuest(questId);
        if (quest == null) {
            quest = new QuestManager.Quest(questId, "Medic Supplies", "Contribute 40 gold to restock the medic's satchel.", 60);
            manager.registerQuest(quest);
        }
        quest.setStatus(QuestManager.Status.ACTIVE);
        context.queueMessage("Quest accepted: Medic Supplies");
        context.queueMessage("Bring " + supplyCost + " gold back to Selene.");
    }

    private void completeQuest(WorldObjectManager.InteractionContext context) {
        QuestManager manager = context.getQuestManager();
        if (manager == null) {
            return;
        }
        QuestManager.Quest quest = manager.getQuest(questId);
        if (quest == null) {
            return;
        }
        if (!context.spendGold(supplyCost)) {
            context.queueMessage("You still need more gold for the supplies.");
            return;
        }
        quest.setStatus(QuestManager.Status.COMPLETED);
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

    private String questText(WorldObjectManager.InteractionContext context) {
        QuestManager manager = context.getQuestManager();
        if (manager == null) {
            return "I am running low on supplies, but I suppose that is not your concern.";
        }
        QuestManager.Quest quest = manager.getQuest(questId);
        if (quest == null || quest.getStatus() == QuestManager.Status.AVAILABLE) {
            return "If you can spare " + supplyCost + " gold, I could restock my supplies.";
        }
        if (quest.getStatus() == QuestManager.Status.ACTIVE) {
            return "Have you gathered the " + supplyCost + " gold yet?";
        }
        if (quest.getStatus() == QuestManager.Status.COMPLETED) {
            return "Thanks again for covering the supply costs. The tinctures are flowing.";
        }
        return "Stay healthy out there.";
    }

    void resetDailySupplies() {
        freeHealsRemaining = freeHealsPerRest;
    }
}
