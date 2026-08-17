package com.warmthdawn.mod.gugu_utils.modularmachenary.requirements.basic;

import com.google.common.collect.Lists;
import com.warmthdawn.mod.gugu_utils.modularmachenary.requirements.types.RequirementTypeAdapter;
import hellfirepvp.modularmachinery.common.crafting.helper.ComponentOutputRestrictor;
import hellfirepvp.modularmachinery.common.crafting.helper.ComponentRequirement;
import hellfirepvp.modularmachinery.common.crafting.helper.CraftCheck;
import hellfirepvp.modularmachinery.common.crafting.helper.ProcessingComponent;
import hellfirepvp.modularmachinery.common.crafting.helper.RecipeCraftingContext;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import hellfirepvp.modularmachinery.common.util.ResultChance;

import javax.annotation.Nonnull;
import java.util.List;

public abstract class RequirementConsumeOnce<T, V extends IResourceToken> extends ComponentRequirementAdapter<T>
        implements ComponentRequirement.Parallelizable {

    V checkToken;
    V outputToken;
    protected int parallelism = 1;
    protected boolean parallelizeUnaffected = false;

    public RequirementConsumeOnce(RequirementTypeAdapter<T> componentType, IOType actionType) {
        super(componentType, actionType);
    }

    @Override
    protected ComponentRequirementAdapter<T> postClone(ComponentRequirementAdapter<T> clone) {
        if (clone instanceof RequirementConsumeOnce) {
            RequirementConsumeOnce<?, ?> parallelClone = (RequirementConsumeOnce<?, ?>) clone;
            parallelClone.parallelizeUnaffected = this.parallelizeUnaffected;
            parallelClone.parallelism = this.parallelizeUnaffected ? 1 : this.parallelism;
        }
        return clone;
    }

    private V createToken(RecipeCraftingContext context, int requestedParallelism) {
        V token = emitConsumptionToken(context);
        token.applyModifiers(context.getModifiers(getRequirementType()), getRequirementType(), getActionType());
        if (!parallelizeUnaffected) {
            token.applyParallelism(Math.max(1, requestedParallelism));
        }
        return token;
    }

    private boolean canSatisfy(List<ProcessingComponent<?>> components,
                               RecipeCraftingContext context,
                               int requestedParallelism) {
        if (ParallelRequirementSupport.isOutputCheckIgnored(this)) {
            return true;
        }
        V token = createToken(context, requestedParallelism);
        return ParallelRequirementSupport.process(components, token, getActionType(), false);
    }

    @Nonnull
    @Override
    public List<ProcessingComponent<?>> copyComponents(List<ProcessingComponent<?>> components) {
        return ParallelRequirementSupport.copyComponents(components);
    }

    @Nonnull
    @Override
    public CraftCheck canStartCrafting(List<ProcessingComponent<?>> components, RecipeCraftingContext context) {
        if (ParallelRequirementSupport.isOutputCheckIgnored(this)) {
            return CraftCheck.success();
        }
        checkToken = createToken(context, parallelism);
        if (ParallelRequirementSupport.process(components, checkToken, getActionType(), false)) {
            return CraftCheck.success();
        }
        return CraftCheck.failure(checkToken.getError() == null
                ? (getActionType() == IOType.INPUT ? getMissingInput() : getMissingOutput())
                : checkToken.getError());
    }

    @Override
    @SuppressWarnings("unchecked")
    public void startCrafting(List<ProcessingComponent<?>> components,
                              RecipeCraftingContext context,
                              ResultChance chance) {
        outputToken = createToken(context, parallelism);
        for (ProcessingComponent<?> component : components) {
            Object provided = component.getProvidedComponent();
            if (provided instanceof ICraftingResourceHolder) {
                ((ICraftingResourceHolder<V>) provided).startCrafting(outputToken);
            }
        }
        if (getActionType() == IOType.INPUT) {
            ParallelRequirementSupport.process(components, outputToken, getActionType(), true);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void finishCrafting(List<ProcessingComponent<?>> components,
                               RecipeCraftingContext context,
                               ResultChance chance) {
        outputToken = createToken(context, parallelism);
        if (getActionType() == IOType.OUTPUT) {
            ParallelRequirementSupport.process(components, outputToken, getActionType(), true);
        }
        for (ProcessingComponent<?> component : components) {
            Object provided = component.getProvidedComponent();
            if (provided instanceof ICraftingResourceHolder) {
                ((ICraftingResourceHolder<V>) provided).finishCrafting(outputToken);
            }
        }
    }

    @Override
    public int getMaxParallelism(List<ProcessingComponent<?>> components,
                                 RecipeCraftingContext context,
                                 int maxParallelism) {
        if (maxParallelism <= 0) {
            return 0;
        }
        if (parallelizeUnaffected || ParallelRequirementSupport.isOutputCheckIgnored(this)) {
            return canSatisfy(components, context, 1) ? maxParallelism : 0;
        }

        int low = 0;
        int high = maxParallelism;
        while (low < high) {
            int middle = low + (high - low + 1) / 2;
            if (canSatisfy(components, context, middle)) {
                low = middle;
            } else {
                high = middle - 1;
            }
        }
        return low;
    }

    /** Compatibility with MMCE releases which used the single-component API. */
    public int maxParallelism(ProcessingComponent<?> component,
                              RecipeCraftingContext context,
                              int maxParallelism) {
        return getMaxParallelism(java.util.Collections.singletonList(component), context, maxParallelism);
    }

    @Override
    public int getParallelism() {
        return parallelism;
    }

    @Override
    public void setParallelism(int parallelism) {
        if (!parallelizeUnaffected) {
            this.parallelism = Math.max(1, parallelism);
        }
    }

    @Override
    public boolean isParallelizeUnaffected() {
        return parallelizeUnaffected;
    }

    @Override
    public void setParallelizeUnaffected(boolean unaffected) {
        this.parallelizeUnaffected = unaffected;
        if (unaffected) {
            this.parallelism = 1;
        }
    }

    @Override
    public boolean isValidComponent(ProcessingComponent<?> component, RecipeCraftingContext ctx) {
        if (!this.isCorrectHatch(component.getComponent())) {
            return false;
        }
        return RequirementUtils.isValidComponent(component, ctx, getActionType());
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean startCrafting(ProcessingComponent<?> component, RecipeCraftingContext context, ResultChance chance) {
        ICraftingResourceHolder<V> handler = (ICraftingResourceHolder<V>) component.getComponent().getContainerProvider();
        handler.startCrafting(checkToken);
        if (handler.isFulfilled()) {
            return false;
        }
        switch (getActionType()) {
            case INPUT:
                handler.consume(outputToken, true);
                if (outputToken.isEmpty()) {
                    handler.setFulfilled(true);
                }
        }
        return false;

    }

    @SuppressWarnings("unchecked")
    @Nonnull
    @Override
    public CraftCheck finishCrafting(ProcessingComponent<?> component, RecipeCraftingContext context, ResultChance chance) {
        ICraftingResourceHolder<V> handler = (ICraftingResourceHolder<V>) component.getComponent().getContainerProvider();
        handler.finishCrafting(outputToken);
        if (handler.isFulfilled()) {
            return CraftCheck.partialSuccess();
        }
        switch (getActionType()) {
            case OUTPUT:
                handler.generate(outputToken, true);
                if (outputToken.isEmpty()) {
                    handler.setFulfilled(true);
                }
        }
        return CraftCheck.partialSuccess();
    }

    @Nonnull
    @Override
    public String getMissingComponentErrorMessage(IOType ioType) {

        switch (ioType) {
            case INPUT:
                return "component.missing." + getRequirementType().getRegistryName() + ".input";
            case OUTPUT:
                return "component.missing." + getRequirementType().getRegistryName() + ".output";
            default:
                return "";
        }

    }

    protected String getMissingInput() {
        return "craftcheck.failure." + getRequirementType().getRegistryName() + ".input";
    }

    protected String getMissingOutput() {
        return "craftcheck.failure." + getRequirementType().getRegistryName() + ".output.space";
    }

    protected String getMiscProblem() {
        return "craftcheck.failure.misc";
    }

    @SuppressWarnings("unchecked")
    @Nonnull
    @Override
    public CraftCheck canStartCrafting(ProcessingComponent<?> component, RecipeCraftingContext context, List<ComponentOutputRestrictor> restrictions) {
        ICraftingResourceHolder<V> handler = (ICraftingResourceHolder<V>) component.getComponent().getContainerProvider();
        switch (getActionType()) {
            case INPUT:
                if (!handler.canConsume()) {
                    return CraftCheck.skipComponent();
                }
                boolean didConsume = handler.consume(checkToken, false);
                if (!didConsume) {
                    return CraftCheck.failure(handler.getInputProblem(checkToken));
                } else if (checkToken.isEmpty()) {
                    return CraftCheck.success();
                } else {
                    return CraftCheck.failure(getMissingInput());
                }
            case OUTPUT:
                if (!handler.canGenerate()) {
                    return CraftCheck.skipComponent();
                }
                boolean didGenerate = handler.generate(checkToken, false);
                if (!didGenerate) {
                    return CraftCheck.failure(handler.getOutputProblem(checkToken));
                } else if (checkToken.isEmpty()) {
                    return CraftCheck.success();
                } else {
                    return CraftCheck.failure(getMissingOutput());
                }
        }
        return CraftCheck.failure(getMiscProblem());
    }

    @Override
    public void startRequirementCheck(ResultChance chance, RecipeCraftingContext context) {
        checkToken = createToken(context, parallelism);
        outputToken = createToken(context, parallelism);
    }

    @Override
    public void endRequirementCheck() {
        checkToken = emitConsumptionToken(null);
        outputToken = emitConsumptionToken(null);
    }

    protected abstract boolean isCorrectHatch(MachineComponent component);

    protected abstract V emitConsumptionToken(RecipeCraftingContext context);
}
