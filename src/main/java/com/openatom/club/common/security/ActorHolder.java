package com.openatom.club.common.security;

/**
 * 当前操作者上下文持有器（ThreadLocal）
 */
public class ActorHolder {

    private static final ThreadLocal<ActorContext> ACTOR_THREAD_LOCAL = new ThreadLocal<>();

    public static void set(ActorContext actor) {
        ACTOR_THREAD_LOCAL.set(actor);
    }

    public static ActorContext get() {
        ActorContext actor = ACTOR_THREAD_LOCAL.get();
        if (actor == null) {
            // 没有传请求头时，视为匿名/普通社员
            return new ActorContext("匿名", null, "社员");
        }
        return actor;
    }

    public static void clear() {
        ACTOR_THREAD_LOCAL.remove();
    }
}
