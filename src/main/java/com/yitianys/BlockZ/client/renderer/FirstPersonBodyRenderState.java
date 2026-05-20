package com.yitianys.BlockZ.client.renderer;

public final class FirstPersonBodyRenderState {
    private static final ThreadLocal<State> STATE = ThreadLocal.withInitial(State::new);

    private FirstPersonBodyRenderState() {
    }

    public static void begin(boolean hideArms, boolean hideHeldItems) {
        State state = STATE.get();
        state.rendering = true;
        state.hideArms = hideArms;
        state.hideHeldItems = hideHeldItems;
    }

    public static void end() {
        State state = STATE.get();
        state.rendering = false;
        state.hideArms = false;
        state.hideHeldItems = false;
    }

    public static boolean isRendering() {
        return STATE.get().rendering;
    }

    public static boolean shouldHideHead() {
        return STATE.get().rendering;
    }

    public static boolean shouldHideArms() {
        State state = STATE.get();
        return state.rendering && state.hideArms;
    }

    public static boolean shouldHideHeldItems() {
        State state = STATE.get();
        return state.rendering && state.hideHeldItems;
    }

    private static final class State {
        private boolean rendering;
        private boolean hideArms;
        private boolean hideHeldItems;
    }
}
