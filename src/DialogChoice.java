class DialogChoice {
    final String text;
    final String nextNodeId;
    final DialogAction action;
    final DialogCondition condition;

    DialogChoice(String text, String nextNodeId) {
        this(text, nextNodeId, null, null);
    }

    DialogChoice(String text, String nextNodeId, DialogAction action, DialogCondition condition) {
        this.text = text;
        this.nextNodeId = nextNodeId;
        this.action = action;
        this.condition = condition;
    }

    boolean isAvailable(InteractionContext context) {
        return condition == null || condition.test(context);
    }

    void execute(InteractionContext context) {
        if (action != null) {
            action.execute(context);
        }
    }
}
