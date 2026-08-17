package com.warmthdawn.mod.gugu_utils.modularmachenary.requirements.basic;

import hellfirepvp.modularmachinery.common.crafting.requirement.type.RequirementType;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.modifier.RecipeModifier;

import java.util.Collection;

public interface IResourceToken {
    void applyModifiers(Collection<RecipeModifier> modifiers, RequirementType type, IOType ioType, float durationMultiplier);

    /**
     * Adjusts the consumable/generatable part of this token for an MMCE
     * parallel craft. Conditions which do not represent an amount may leave
     * the token unchanged.
     */
    void applyParallelism(int parallelism);

    static int multiplyClamped(int value, int multiplier) {
        if (value <= 0 || multiplier <= 0) {
            return 0;
        }
        return (int) Math.min(Integer.MAX_VALUE, (long) value * multiplier);
    }

    default void applyModifiers(Collection<RecipeModifier> modifiers, RequirementType type, IOType ioType) {
        applyModifiers(modifiers, type, ioType, 1.0f);
    }

    String getKey();

    default String getError() {
        return null;
    }


    boolean isEmpty();
}
