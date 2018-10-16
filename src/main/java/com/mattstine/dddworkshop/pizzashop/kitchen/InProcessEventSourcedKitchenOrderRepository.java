package com.mattstine.dddworkshop.pizzashop.kitchen;

import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.EventLog;
import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.Topic;
import com.mattstine.dddworkshop.pizzashop.infrastructure.repository.adapters.InProcessEventSourcedRepository;
import com.mattstine.dddworkshop.pizzashop.ordering.OnlineOrderRef;

import java.util.HashMap;
import java.util.Map;

final class InProcessEventSourcedKitchenOrderRepository extends InProcessEventSourcedRepository<KitchenOrderRef, KitchenOrder, KitchenOrder.OrderState, KitchenOrderEvent, KitchenOrderAddedEvent> implements KitchenOrderRepository {

    Map<OnlineOrderRef, KitchenOrderRef> onlineOrderRefToKitchenOrderRef;

    InProcessEventSourcedKitchenOrderRepository(EventLog eventLog, Topic topic) {
        super(eventLog,
                KitchenOrderRef.class,
                KitchenOrder.class,
                KitchenOrder.OrderState.class,
                KitchenOrderAddedEvent.class,
                topic);

        onlineOrderRefToKitchenOrderRef = new HashMap<>();

        eventLog.subscribe(new Topic("kitchen_orders"), e -> {
            if (e instanceof KitchenOrderAddedEvent) {
                KitchenOrderAddedEvent koae = (KitchenOrderAddedEvent) e;
                onlineOrderRefToKitchenOrderRef.put(koae.getState().getOnlineOrderRef(), koae.getRef());
            }
        });
    }

    @Override
    public KitchenOrder findByOnlineOrderRef(OnlineOrderRef onlineOrderRef) {
        KitchenOrderRef kitchenOrderRef = onlineOrderRefToKitchenOrderRef.get(onlineOrderRef);
        if (kitchenOrderRef != null) {
            return this.findByRef(kitchenOrderRef);
        }
        return null;
    }
}
