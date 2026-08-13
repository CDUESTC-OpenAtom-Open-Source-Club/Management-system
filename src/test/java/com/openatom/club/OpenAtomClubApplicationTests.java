package com.openatom.club;

import com.openatom.club.common.security.ActorContext;
import com.openatom.club.common.security.ActorHolder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 主启动类基础验证（不加载完整 Spring 上下文，无需数据库连接）
 * 完整集成测试请在有 PostgreSQL 的环境下运行：
 *   docker compose up -d && mvn test -Dspring.profiles.active=integration
 */
class OpenAtomClubApplicationTests {

    @Test
    void mainClassExists() {
        // 验证主类可以被加载
        assertNotNull(OpenAtomClubApplication.class);
    }

    @Test
    void actorContextPermissions() {
        // 会长拥有全部权限
        ActorContext president = new ActorContext("会长", "会长室", "会长");
        assertTrue(president.isPresident());
        assertTrue(president.isFullAccess());
        assertTrue(president.isAdmin());
        assertTrue(president.hasManagePermission());
    }

    @Test
    void actorContextVicePresident() {
        // 副会长拥有全部权限
        ActorContext vp = new ActorContext("副会长", "办公室", "副会长");
        assertTrue(vp.isVicePresident());
        assertTrue(vp.isFullAccess());
        assertTrue(vp.isAdmin());
        assertTrue(vp.hasManagePermission());
    }

    @Test
    void actorContextSecretary() {
        // 秘书处成员也拥有全部权限
        ActorContext secretary = new ActorContext("秘书", "秘书处", "部长");
        assertTrue(secretary.isSecretary());
        assertTrue(secretary.isFullAccess());
        assertTrue(secretary.isAdmin());
        assertTrue(secretary.hasManagePermission());
    }

    @Test
    void actorContextOrdinaryMember() {
        // 普通社员无管理权限
        ActorContext member = new ActorContext("张三", "技术部", "社员");
        assertFalse(member.isFullAccess());
        assertFalse(member.isAdmin());
        assertFalse(member.isSecretary());
        assertFalse(member.hasManagePermission());
    }

    @Test
    void actorHolderThreadLocal() {
        try {
            ActorContext actor = new ActorContext("测试", "秘书处", "部长");
            ActorHolder.set(actor);
            ActorContext retrieved = ActorHolder.get();
            assertEquals("测试", retrieved.getName());
            assertEquals("秘书处", retrieved.getDepartment());
            assertTrue(retrieved.isFullAccess());
            assertTrue(retrieved.hasManagePermission());
        } finally {
            ActorHolder.clear();
        }
    }

    @Test
    void actorHolderDefaultContext() {
        ActorHolder.clear();
        ActorContext defaultActor = ActorHolder.get();
        assertNotNull(defaultActor);
        assertEquals("社员", defaultActor.getPosition());
        assertFalse(defaultActor.isFullAccess());
        assertFalse(defaultActor.hasManagePermission());
    }
}
