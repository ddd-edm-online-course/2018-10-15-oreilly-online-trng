package com.mattstine.dddworkshop.pizzashop.kitchen;

import com.mattstine.dddworkshop.pizzashop.infrastructure.events.adapters.InProcessEventLog;
import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.EventLog;
import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.Topic;
import com.mattstine.dddworkshop.pizzashop.infrastructure.repository.ports.Aggregate;
import com.mattstine.dddworkshop.pizzashop.infrastructure.repository.ports.AggregateState;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.NonFinal;

import java.util.function.BiFunction;

@Value
public final class Pizza implements Aggregate {
    PizzaRef ref;
    KitchenOrderRef kitchenOrderRef;
    Size size;
    EventLog $eventLog;
    @NonFinal
    State state;

    @Builder
    private Pizza(@NonNull PizzaRef ref,
                  @NonNull KitchenOrderRef kitchenOrderRef,
                  @NonNull Size size,
                  @NonNull EventLog eventLog) {
        this.ref = ref;
        this.kitchenOrderRef = kitchenOrderRef;
        this.size = size;
        this.$eventLog = eventLog;

        this.state = State.NEW;
    }

    /**
     * Private no-args ctor to support reflection ONLY.
     */
    @SuppressWarnings("unused")
    private Pizza() {
        this.ref = null;
        this.kitchenOrderRef = null;
        this.size = null;
        this.$eventLog = null;
    }

    public boolean isNew() {
        return this.state == State.NEW;
    }

    void startPrep() {
        if (!this.isNew()) {
            throw new IllegalStateException();
        }

        this.state = State.PREPPING;
        this.$eventLog.publish(new Topic("pizzas"), new PizzaPrepStartedEvent(this.ref));
    }

    boolean isPrepping() {
        return this.state == State.PREPPING;
    }

    void finishPrep() {
        if (!this.isPrepping()) {
            throw new IllegalStateException();
        }

        this.state = State.PREPPED;
        this.$eventLog.publish(new Topic("pizzas"), new PizzaPrepFinishedEvent(this.ref));
    }

    boolean hasFinishedPrep() {
        return this.state == State.PREPPED;
    }

    void startBake() {
        if (!this.hasFinishedPrep()) {
            throw new IllegalStateException();
        }

        this.state = State.BAKING;
        this.$eventLog.publish(new Topic("pizzas"), new PizzaBakeStartedEvent(this.ref));
    }

    boolean isBaking() {
        return this.state == State.BAKING;
    }

    void finishBake() {
        if (!this.isBaking()) {
            throw new IllegalStateException();
        }

        this.state = State.BAKED;
        this.$eventLog.publish(new Topic("pizzas"), new PizzaBakeFinishedEvent(this.ref));
    }

    boolean hasFinishedBaking() {
        return this.state == State.BAKED;
    }

    @Override
    public Pizza identity() {
        return Pizza.builder()
                .ref(PizzaRef.IDENTITY)
                .eventLog(EventLog.IDENTITY)
                .kitchenOrderRef(KitchenOrderRef.IDENTITY)
                .size(Size.IDENTITY)
                .build();
    }

    @Override
    public BiFunction<Pizza, PizzaEvent, Pizza> accumulatorFunction() {
        return new Accumulator();
    }

    @Override
    public PizzaRef getRef() {
        return ref;
    }

    @Override
    public PizzaState state() {
        return new PizzaState(this.ref, this.kitchenOrderRef, this.size);
    }

    enum Size {
        IDENTITY, SMALL, MEDIUM, LARGE
    }

    enum State {
        NEW,
        PREPPING,
        PREPPED,
        BAKING,
        BAKED
    }

    private static class Accumulator implements BiFunction<Pizza, PizzaEvent, Pizza> {

        @Override
        public Pizza apply(Pizza pizza, PizzaEvent pizzaEvent) {
            if (pizzaEvent instanceof PizzaAddedEvent) {
                PizzaAddedEvent pae = (PizzaAddedEvent) pizzaEvent;
                PizzaState pizzaState = pae.getState();
                return Pizza.builder()
                        .size(pizzaState.getSize())
                        .ref(pizzaState.getRef())
                        .kitchenOrderRef(pizzaState.getKitchenOrderRef())
                        .eventLog(InProcessEventLog.instance())
                        .build();
            } else if (pizzaEvent instanceof PizzaPrepStartedEvent) {
                pizza.state = State.PREPPING;
                return pizza;
            } else if (pizzaEvent instanceof PizzaPrepFinishedEvent) {
                pizza.state = State.PREPPED;
                return pizza;
            } else if (pizzaEvent instanceof PizzaBakeStartedEvent) {
                pizza.state = State.BAKING;
                return pizza;
            } else if (pizzaEvent instanceof PizzaBakeFinishedEvent) {
                pizza.state = State.BAKED;
                return pizza;
            }
            throw new IllegalStateException("Unknown PizzaEvent");
        }
    }

    @Value
    static class PizzaState implements AggregateState {
        PizzaRef ref;
        KitchenOrderRef kitchenOrderRef;
        Size size;
    }
}
