/* Adapted from Hephaestus, Slime Knights and contributors (MIT). */
package com.radi.radisconstruct.tools;

import com.radi.radisconstruct.material.*;
import java.util.*;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/**
 * Original ordered part assembly, including two separate unstackable daggers. Server station
 * integrations share this validated, non-mutating construction API.
 */
public final class OriginalToolAssembly {
  private OriginalToolAssembly() {}

  public static List<ItemStack> create(String tool, List<ItemStack> input) {
    var parts = OriginalToolCatalog.parts(tool);
    if (parts.isEmpty() || input == null || parts.size() != input.size())
      throw new IllegalArgumentException("Wrong original part layout");
    var materials = new ArrayList<String>();
    for (int i = 0; i < parts.size(); i++) {
      var stack = input.get(i);
      var material = MaterialStackData.read(stack);
      if (stack.isEmpty()
          || stack.getItem() != ToolPartItems.all().get(parts.get(i))
          || material == null
          || material.hasVariant()
          || !material.namespace().equals("tconstruct")
          || !OriginalToolCatalog.compatible(parts.get(i)).contains(material.path()))
        throw new IllegalArgumentException("Incompatible ordered original part/material");
      materials.add(material.path());
    }
    var result = MaterialToolSupport.create(tool, materials);
    var outputs = new ArrayList<ItemStack>();
    for (int i = 0; i < OriginalToolCatalog.outputCount(tool); i++) outputs.add(result.copy());
    return List.copyOf(outputs);
  }

  public static final class Pending {
    private final Container owner;
    private final String tool;
    private final List<Integer> inputs, outputs;
    private final List<ItemStack> snapshots, results;
    private boolean committed;

    private Pending(
        Container owner,
        String tool,
        List<Integer> inputs,
        List<Integer> outputs,
        List<ItemStack> snapshots,
        List<ItemStack> results) {
      this.owner = owner;
      this.tool = tool;
      this.inputs = List.copyOf(inputs);
      this.outputs = List.copyOf(outputs);
      this.snapshots = snapshots;
      this.results = results;
    }

    public List<ItemStack> results() {
      return results.stream().map(ItemStack::copy).toList();
    }
  }

  public static Pending preview(
      Container inventory, String tool, List<Integer> inputs, List<Integer> outputs) {
    if (inventory == null
        || inputs == null
        || outputs == null
        || inputs.size() != OriginalToolCatalog.parts(tool).size()
        || outputs.size() != OriginalToolCatalog.outputCount(tool)) return null;
    var unique = new HashSet<Integer>();
    for (var list : List.of(inputs, outputs))
      for (var i : list)
        if (i == null || i < 0 || i >= inventory.getContainerSize() || !unique.add(i)) return null;
    if (outputs.stream().anyMatch(i -> !inventory.getItem(i).isEmpty())) return null;
    var snapshots = inputs.stream().map(i -> inventory.getItem(i).copy()).toList();
    try {
      return new Pending(inventory, tool, inputs, outputs, snapshots, create(tool, snapshots));
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  public static boolean commit(Container inventory, Pending pending) {
    if (pending == null || pending.committed || inventory != pending.owner) return false;
    if (pending.outputs.stream().anyMatch(i -> !inventory.getItem(i).isEmpty())) return false;
    for (int i = 0; i < pending.inputs.size(); i++)
      if (!ItemStack.matches(inventory.getItem(pending.inputs.get(i)), pending.snapshots.get(i)))
        return false;
    var rebuilt = create(pending.tool, pending.snapshots);
    for (int i = 0; i < rebuilt.size(); i++)
      if (!ItemStack.matches(rebuilt.get(i), pending.results.get(i))) return false;
    pending.committed = true;
    for (var i : pending.inputs) {
      var next = inventory.getItem(i).copy();
      next.shrink(1);
      inventory.setItem(i, next);
    }
    for (int i = 0; i < pending.outputs.size(); i++)
      inventory.setItem(pending.outputs.get(i), rebuilt.get(i));
    inventory.setChanged();
    return true;
  }
}
