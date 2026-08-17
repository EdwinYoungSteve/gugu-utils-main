package com.warmthdawn.mod.gugu_utils.modularmachenary.requirements.basic;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.warmthdawn.mod.gugu_utils.modularmachenary.requirements.RequirementAspect;
import com.warmthdawn.mod.gugu_utils.modularmachenary.requirements.types.RequirementTypeAdapter;
import hellfirepvp.modularmachinery.common.crafting.helper.ComponentOutputRestrictor;
import hellfirepvp.modularmachinery.common.crafting.helper.ComponentRequirement;
import hellfirepvp.modularmachinery.common.crafting.helper.CraftCheck;
import hellfirepvp.modularmachinery.common.crafting.helper.ProcessingComponent;
import hellfirepvp.modularmachinery.common.crafting.helper.RecipeCraftingContext;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import hellfirepvp.modularmachinery.common.util.ResultChance;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.List;

public abstract class RequirementConsumePerTick<T, V extends IResourceToken> extends ComponentRequirementAdapter.PerTick<T>
        implements ComponentRequirement.Parallelizable {
    protected V checkToken;

    protected V perTickToken;
    private int totalTick;
    protected int parallelism = 1;
    protected boolean parallelizeUnaffected = false;

    public RequirementConsumePerTick(RequirementTypeAdapter<T> requirementType, IOType actionType) {
        super(requirementType, actionType);
    }

    @Override
    protected ComponentRequirementAdapter.PerTick<T> postClone(ComponentRequirementAdapter.PerTick<T> clone) {
        if (clone instanceof RequirementConsumePerTick) {
            RequirementConsumePerTick<?, ?> parallelClone = (RequirementConsumePerTick<?, ?>) clone;
            parallelClone.parallelizeUnaffected = this.parallelizeUnaffected;
            parallelClone.parallelism = this.parallelizeUnaffected ? 1 : this.parallelism;
        }
        return clone;
    }

    private V createToken(RecipeCraftingContext context, int requestedParallelism, float durationMultiplier) {
        V token = emitConsumptionToken(context);
        token.applyModifiers(context.getModifiers(getRequirementType()), getRequirementType(), getActionType(), durationMultiplier);
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
        V token = createToken(context, requestedParallelism, 1.0F);
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
        checkToken = createToken(context, parallelism, 1.0F);
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
        checkToken = createToken(context, parallelism, 1.0F);
        for (ProcessingComponent<?> component : components) {
            Object provided = component.getProvidedComponent();
            if (provided instanceof ICraftingResourceHolder) {
                ((ICraftingResourceHolder<V>) provided).startCrafting(checkToken);
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void finishCrafting(List<ProcessingComponent<?>> components,
                               RecipeCraftingContext context,
                               ResultChance chance) {
        for (ProcessingComponent<?> component : components) {
            Object provided = component.getProvidedComponent();
            if (provided instanceof ICraftingResourceHolder) {
                ((ICraftingResourceHolder<V>) provided).finishCrafting(checkToken);
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
    public boolean startCrafting(ProcessingComponent<?> processingComponent, RecipeCraftingContext recipeCraftingContext, ResultChance resultChance) {
        boolean canStart = canStartCrafting(processingComponent, recipeCraftingContext, Lists.newArrayList()).isSuccess();
        if(canStart) {
            ICraftingResourceHolder<V> handler = (ICraftingResourceHolder<V>) processingComponent.getProvidedComponent();
            handler.startCrafting(checkToken);
        }

        return false;
    }

    @NotNull
    @Override
    @SuppressWarnings("unchecked")
    public CraftCheck finishCrafting(ProcessingComponent<?> processingComponent, RecipeCraftingContext recipeCraftingContext, ResultChance resultChance) {
        if (processingComponent.getProvidedComponent() instanceof ICraftingResourceHolder) {
            ICraftingResourceHolder<V> handler = (ICraftingResourceHolder<V>) processingComponent.getProvidedComponent();
            handler.finishCrafting(checkToken);
        }
        return CraftCheck.partialSuccess();
    }

    protected String getMissingInput() {
        return "craftcheck.failure." + getRequirementType().getRegistryName() + ".input";
    }

    protected String getMissingOutput() {
        return "craftcheck.failure." + getRequirementType().getRegistryName() + ".output";
    }

    protected String getMiscProblem() {
        return "craftcheck.failure.misc";
    }

    public V getPerTickToken() {
        return perTickToken;
    }

    @SuppressWarnings("unchecked")
    @Override
    public CraftCheck canStartCrafting(ProcessingComponent<?> component, RecipeCraftingContext context, List<ComponentOutputRestrictor> list) {
        if (!isCorrectHatch(component.getComponent())) return CraftCheck.skipComponent();
        ICraftingResourceHolder<V> handler = (ICraftingResourceHolder<V>) component.getProvidedComponent();
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
                    return CraftCheck.partialSuccess();
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
                    return CraftCheck.partialSuccess();
                }
        }
        return CraftCheck.failure(getMiscProblem());
    }

    @Override
    public void startRequirementCheck(ResultChance chance, RecipeCraftingContext context) {
        checkToken = createToken(context, parallelism, 1.0F);
    }

    @Override
    public void endRequirementCheck() {
        checkToken = emitConsumptionToken(null);
    }

    @NotNull
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

    @Override
    public CraftCheck resetIOTick(RecipeCraftingContext context) {
        boolean enough = perTickToken != null && perTickToken.isEmpty();
        String error = perTickToken == null ? null : perTickToken.getError();
        this.perTickToken = emitConsumptionToken(context);
        if(!Strings.isNullOrEmpty(error)){
            return CraftCheck.failure(error);
        }
        else if (enough)
            return CraftCheck.success();
        else if (getActionType() == IOType.INPUT)
            return CraftCheck.failure(getMissingInput());
        else
            return CraftCheck.failure(getMissingOutput());
    }

    @Override
    public void startIOTick(RecipeCraftingContext context, float durationMultiplier) {
        perTickToken.applyModifiers(context.getModifiers(getRequirementType()), getRequirementType(), getActionType(), durationMultiplier);
        if (!parallelizeUnaffected) {
            perTickToken.applyParallelism(parallelism);
        }
    }

    protected abstract V emitConsumptionToken(RecipeCraftingContext context);

    protected abstract boolean isCorrectHatch(MachineComponent component);

    @SuppressWarnings("unchecked")
    @Nonnull
    @Override
    public CraftCheck doIOTick(ProcessingComponent<?> component, RecipeCraftingContext context) {


        if (!isCorrectHatch(component.getComponent())) return CraftCheck.skipComponent();
        ICraftingResourceHolder<V> handler = (ICraftingResourceHolder<V>) component.getProvidedComponent();
        switch (getActionType()) {
            case INPUT:
                if (!handler.canConsume()) {
                    return CraftCheck.skipComponent();
                }
                boolean didConsume = handler.consume(perTickToken, true);
                if (!didConsume) {
                    return CraftCheck.failure(handler.getInputProblem(perTickToken));
                } else if (perTickToken.isEmpty()) {
                    return CraftCheck.success();
                } else {
                    return CraftCheck.partialSuccess();
                }

            case OUTPUT:
                if (!handler.canGenerate()) {
                    return CraftCheck.skipComponent();
                }
                boolean didGenerate = handler.generate(perTickToken, true);
                if (!didGenerate) {
                    return CraftCheck.failure(handler.getOutputProblem(perTickToken));
                } else if (perTickToken.isEmpty()) {
                    return CraftCheck.success();
                } else {
                    return CraftCheck.partialSuccess();
                }
        }
        //This is neither input nor output? when do we actually end up in this case down here?
        return CraftCheck.failure(getMiscProblem());
    }

    protected int getTotalTick() {
        return this.totalTick;
    }

    public void setTotalTick(int totalTick) {
        this.totalTick = totalTick;
    }
}
