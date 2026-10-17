package slimeknights.tconstruct.smeltery.block.entity;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.*;
import net.minecraft.world.level.storage.*;
import org.junit.jupiter.api.*;
import slimeknights.tconstruct.smeltery.SmelteryContent;
import static org.junit.jupiter.api.Assertions.*;

class NativeHeaterPersistenceTest {
    @BeforeAll static void bootstrap() throws Exception { com.radi.radisconstruct.PortedContentTest.bootstrap(); }
    @Test void nativeSaveAndTransactionalExtractionPreserveFuelStackComponents() {
        var registry = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        var reporter = net.minecraft.util.ProblemReporter.DISCARDING;
        var coal = new ItemStack(Items.COAL, 12);
        coal.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Heater fuel"));
        var data = TagValueOutput.createWithContext(reporter, registry); data.store("item", ItemStack.OPTIONAL_CODEC, coal);
        var entity = new NativeHeaterBlockEntity(BlockPos.ZERO, SmelteryContent.HEATER.defaultBlockState());
        entity.loadAdditional(TagValueInput.create(reporter, registry, data.buildResult()));
        var resource = ItemVariant.of(coal);
        try (var tx = Transaction.openOuter()) { assertEquals(5, entity.items().extract(resource, 5, tx)); }
        assertEquals(12, entity.items().getAmount());
        try (var tx = Transaction.openOuter()) { assertEquals(5, entity.items().extract(resource, 5, tx)); tx.commit(); }
        var saved = TagValueOutput.createWithContext(reporter, registry); entity.saveAdditional(saved);
        var restored = TagValueInput.create(reporter, registry, saved.buildResult()).read("item", ItemStack.OPTIONAL_CODEC).orElseThrow();
        assertEquals(7, restored.getCount()); assertTrue(ItemStack.isSameItemSameComponents(coal, restored));
    }
}
