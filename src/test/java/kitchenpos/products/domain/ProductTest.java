package kitchenpos.products.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.math.BigDecimal;

import kitchenpos.Fixtures;
import kitchenpos.products.application.FakeProfanityValidator;
import kitchenpos.products.domain.tobe.ProfanityValidator;
import kitchenpos.products.domain.tobe.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("상품 테스트")
class ProductTest {

  public static final String UDON = "udon";
  public static final long VAL = 10_000L;
  private ProfanityValidator profanityValidator;

  @BeforeEach
  void setUp(){
    profanityValidator = new FakeProfanityValidator();
  }

  @DisplayName("상품을 생성할 수 있다.")
  @Test
  void createProduct(){
    Product actual = Product.from(UDON, VAL, profanityValidator);

    assertAll(
        () -> assertThat(actual.getProductPrice()).isEqualTo(BigDecimal.valueOf(VAL)),
        () -> assertThat(actual.getProductName()).isEqualTo(UDON),
        () -> assertThat(actual.getId()).isNotNull()
    );
  }

  @DisplayName("상품의 가격을 변경할 수 있다.")
  @Test
  void changeProductPrice(){
    Product actual = Fixtures.createProduct();

    actual.changeProductPrice(20_000L);

    assertThat(actual.getProductPrice()).isEqualTo(BigDecimal.valueOf(20_000L));
  }

}
