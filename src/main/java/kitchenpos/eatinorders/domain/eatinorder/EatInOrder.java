package kitchenpos.eatinorders.domain.eatinorder;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import kitchenpos.common.domain.orders.OrderStatus;
import kitchenpos.common.domain.ordertables.OrderType;

@Entity
@DiscriminatorValue("EAT_IN")
public class EatInOrder extends Order {

  protected EatInOrder() {
    super();
  }

  @Override
  public void accept(KitchenridersClient kitchenridersClient) {
    throw new IllegalStateException("해당 주문 타입은 라이더에게 전달되지 않아도 됩니다.");
  }

  @Override
  public void delivering() {
    throw new IllegalStateException("해당 주문 타입은 라이더에게 전달되지 않아도 됩니다.");

  }

  @Override
  public void delivered() {
    throw new IllegalStateException("해당 주문 타입은 라이더에게 전달되지 않아도 됩니다.");
  }

  protected EatInOrder(OrderStatus orderStatus, OrderLineItems orderLineItems, OrderTable orderTable) {
    super(OrderType.EAT_IN, orderStatus, orderLineItems, orderTable);
  }

  @Override
  public void complete() {
    if (status != OrderStatus.SERVED) {
      throw new IllegalStateException(" `주문 상태`가 `접수(ACCEPTED)`이 아닌 주문은 전달할 수 없습니다.");
    }


    status = OrderStatus.COMPLETED;
  }

}
