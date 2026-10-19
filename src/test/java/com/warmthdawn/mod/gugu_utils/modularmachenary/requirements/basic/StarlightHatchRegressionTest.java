package com.warmthdawn.mod.gugu_utils.modularmachenary.requirements.basic;

import com.warmthdawn.mod.gugu_utils.modularmachenary.requirements.RequirementStarlight;
import com.warmthdawn.mod.gugu_utils.modularmachenary.starlight.TileStarlightInputHatch;
import hellfirepvp.astralsorcery.common.constellation.IConstellation;
import hellfirepvp.modularmachinery.common.crafting.helper.CraftCheck;
import hellfirepvp.modularmachinery.common.crafting.helper.ProcessingComponent;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class StarlightHatchRegressionTest {
    private static final String WRONG_CONSTELLATION =
        "craftcheck.failure.gugu-utils:starlight.input.wrong_constellation";
    private static final String INSUFFICIENT_STARLIGHT =
        "craftcheck.failure.gugu-utils:starlight.input";
    private final IConstellation expected = constellation();
    private final IConstellation other = constellation();

    @Test
    public void wrongHatchBeforeMatchingHatchDoesNotPoisonTick() {
        assertTickSucceeds(Arrays.asList(hatch(other, 1000), hatch(expected, 1000)));
    }

    @Test
    public void matchingHatchBeforeWrongHatchAlsoSucceeds() {
        assertTickSucceeds(Arrays.asList(hatch(expected, 1000), hatch(other, 1000)));
    }

    @Test
    public void recipeStartAndParallelProbeAcceptLaterMatchingHatch() {
        List<ProcessingComponent<?>> hatches = Arrays.asList(hatch(other, 1000), hatch(expected, 1000));
        RequirementStarlight.RT token = new RequirementStarlight.RT(500, expected);
        assertTrue(ParallelRequirementSupport.process(hatches, token, IOType.INPUT, false));
        assertTrue(token.isEmpty());
        assertNull(token.getError());

        TestRequirement requirement = new TestRequirement(expected);
        requirement.prepareCheck();
        assertFalse(requirement.canStartCrafting(hatches.get(0), null, Collections.emptyList()).isSuccess());
        assertTrue(requirement.canStartCrafting(hatches.get(1), null, Collections.emptyList()).isSuccess());
    }

    @Test
    public void missingConstellationStillFails() {
        TestRequirement requirement = new TestRequirement(expected);
        requirement.resetIOTick(null);
        assertFalse(requirement.doIOTick(hatch(other, 1000), null).isSuccess());
        assertEquals(WRONG_CONSTELLATION, requirement.resetIOTick(null).getUnlocalizedMessage());
    }

    @Test
    public void matchingButUnderpoweredHatchStillFails() {
        TestRequirement requirement = new TestRequirement(expected);
        requirement.resetIOTick(null);
        requirement.doIOTick(hatch(other, 1000), null);
        assertFalse(requirement.doIOTick(hatch(expected, 499), null).isSuccess());
        assertEquals(INSUFFICIENT_STARLIGHT, requirement.resetIOTick(null).getUnlocalizedMessage());
    }

    @Test
    public void unrestrictedStarlightStillMatchesAnyConstellation() {
        RequirementStarlight.RT token = new RequirementStarlight.RT(500, null);
        assertTrue(ParallelRequirementSupport.process(Collections.singletonList(hatch(other, 500)),
            token, IOType.INPUT, false));
        assertNull(token.getError());
    }

    @Test
    public void starlightRemainsAThresholdAndIsNotConsumedOrMultiplied() {
        ProcessingComponent<?> component = hatch(expected, 500);
        TestRequirement requirement = new TestRequirement(expected);
        requirement.setParallelism(64);
        assertEquals(1, requirement.getParallelism());
        for (int tick = 0; tick < 3; tick++) {
            requirement.resetIOTick(null);
            assertTrue(requirement.doIOTick(component, null).isSuccess());
            assertTrue(requirement.resetIOTick(null).isSuccess());
        }
    }

    private void assertTickSucceeds(List<ProcessingComponent<?>> components) {
        TestRequirement requirement = new TestRequirement(expected);
        // Reproduce MMCE's component loop and final resetIOTick, which checks
        // both the remaining threshold and the shared token's error field.
        for (int tick = 0; tick < 3; tick++) {
            requirement.resetIOTick(null);
            for (ProcessingComponent<?> component : components) {
                CraftCheck check = requirement.doIOTick(component, null);
                if (check.isSuccess()) break;
            }
            assertTrue(requirement.resetIOTick(null).isSuccess());
        }
    }

    private ProcessingComponent<?> hatch(IConstellation constellation, int amount) {
        TileStarlightInputHatch hatch = new TileStarlightInputHatch() {
            @Override
            public int getStarlightStored() {
                return amount;
            }
        };
        hatch.setFocusedConstellation(constellation);
        return new ProcessingComponent<>(null, new CraftingResourceHolder<>(hatch), null);
    }

    private IConstellation constellation() {
        return (IConstellation) Proxy.newProxyInstance(IConstellation.class.getClassLoader(),
            new Class<?>[]{IConstellation.class}, (proxy, method, args) -> {
                if (method.getName().equals("equals")) return proxy == args[0];
                if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
                return null;
            });
    }

    private static class TestRequirement extends RequirementStarlight {
        TestRequirement(IConstellation constellation) {
            super(500, constellation, IOType.INPUT);
        }

        void prepareCheck() {
            checkToken = emitConsumptionToken(null);
        }

        @Override
        protected boolean isCorrectHatch(MachineComponent component) {
            // The fixtures contain only starlight holders; no game registry
            // or world is needed to exercise the real matching and tick code.
            return true;
        }

        @Override
        protected String getMissingInput() {
            return INSUFFICIENT_STARLIGHT;
        }
    }
}
