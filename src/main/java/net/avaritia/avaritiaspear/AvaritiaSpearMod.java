package net.avaritia.avaritiaspear;

import net.avaritia.avaritiaspear.init.AvaritiaSpearModItems;
import net.minecraft.server.TickTask;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.util.thread.SidedThreadGroups;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unimi.dsi.fastutil.ints.IntObjectImmutablePair;
import it.unimi.dsi.fastutil.ints.IntObjectPair;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 主类。
 * <p>
 * 1.21 那份里的网络模块（{@code MESSAGES} / {@code addNetworkMessage} / {@code registerNetworking}）
 * 从来没被填充过，属于 MCreator 模板产物；{@code CuriosApiHelper} 同样无人调用。
 * 这两块都依赖 NeoForge 专有 API，且是死代码，移植时直接删掉。
 */
@Mod("avaritia_spear")
public class AvaritiaSpearMod {
    public static final Logger LOGGER = LogManager.getLogger(AvaritiaSpearMod.class);
    public static final String MODID = "avaritia_spear";

    public AvaritiaSpearMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        MinecraftForge.EVENT_BUS.register(this);
        // Start of user code block mod init
        AvaritiaSpearModItems.REGISTRY.register(modEventBus);
        // End of user code block mod init
    }

    // ==================== MCreator 的延时任务工具 ====================
    private static final Queue<IntObjectPair<Runnable>> workToBeScheduled = new ConcurrentLinkedQueue<>();
    private static final PriorityQueue<TickTask> workQueue =
            new PriorityQueue<>(Comparator.comparingInt(TickTask::getTick));

    public static void queueServerWork(int delay, Runnable action) {
        if (Thread.currentThread().getThreadGroup() == SidedThreadGroups.SERVER)
            workToBeScheduled.add(new IntObjectImmutablePair<>(delay, action));
    }

    @SubscribeEvent
    public void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        int currentTick = event.getServer().getTickCount();
        IntObjectPair<Runnable> work;
        while ((work = workToBeScheduled.poll()) != null) {
            workQueue.add(new TickTask(currentTick + work.leftInt(), work.right()));
        }
        while (!workQueue.isEmpty() && currentTick >= workQueue.peek().getTick()) {
            workQueue.poll().run();
        }
    }
}