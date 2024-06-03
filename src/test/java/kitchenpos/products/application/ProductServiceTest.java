package kitchenpos.products.application;

import static kitchenpos.Fixtures.menu;
import static kitchenpos.Fixtures.menuProduct;
import static kitchenpos.Fixtures.product;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import kitchenpos.menus.application.InMemoryMenuRepository;
import kitchenpos.menus.domain.Menu;
import kitchenpos.menus.domain.MenuRepository;
import kitchenpos.products.domain.tobe.Product;
import kitchenpos.products.domain.tobe.ProductRepository;
import kitchenpos.products.domain.tobe.ProfanityValidator;
import kitchenpos.products.application.dto.ProductRequest;
import kitchenpos.products.infra.FakeProfanityValidator;
import kitchenpos.products.infra.InMemoryProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class ProductServiceTest {

  private ProductRepository productRepository;
  private MenuRepository menuRepository;
  private ProfanityValidator profanityValidator;
  private ProductService productService;

  @BeforeEach
  void setUp() {
    productRepository = new InMemoryProductRepository();
    menuRepository = new InMemoryMenuRepository();
    profanityValidator = new FakeProfanityValidator();
    productService = new ProductService(productRepository, menuRepository, profanityValidator);
  }

  @DisplayName("상품을 등록할 수 있다.")
  @Test
  void create() {
    final ProductRequest expected = createProductRequest("후라이드", 16_000L);
    final Product actual = productService.create(expected);
    assertThat(actual).isNotNull();
    assertAll(
        () -> assertThat(actual.getId()).isNotNull(),
        () -> assertThat(actual.getProductName()).isEqualTo(expected.getName()),
        () -> assertThat(actual.getProductPrice()).isEqualTo(BigDecimal.valueOf(expected.getPrice()))
    );
  }

  @DisplayName("상품의 가격이 올바르지 않으면 등록할 수 없다.")
  @ValueSource(longs = -1000L)
  @NullSource
  @ParameterizedTest
  void create(final Long price) {
    final ProductRequest expected = createProductRequest("후라이드", price);
    assertThatThrownBy(() -> productService.create(expected))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @DisplayName("상품의 이름이 올바르지 않으면 등록할 수 없다.")
  @ValueSource(strings = {"비속어", "욕설이 포함된 이름"})
  @NullSource
  @ParameterizedTest
  void create(final String name) {
    final ProductRequest expected = createProductRequest(name, 16_000L);
    assertThatThrownBy(() -> productService.create(expected))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @DisplayName("상품의 가격을 변경할 수 있다.")
  @Test
  void changePrice() {
    final UUID productId = productRepository.save(product("후라이드", 16_000L)).getId();
    final ProductRequest expected = changePriceRequest(15_000L);
    final Product actual = productService.changePrice(productId, expected);
    assertThat(actual.getProductPrice()).isEqualTo(BigDecimal.valueOf(expected.getPrice()));
  }

  @DisplayName("상품의 가격이 올바르지 않으면 변경할 수 없다.")
  @ValueSource(longs = -1000L)
  @NullSource
  @ParameterizedTest
  void changePrice(final Long price) {
    final UUID productId = productRepository.save(product("후라이드", 16_000L)).getId();
    final ProductRequest expected = changePriceRequest(price);
    assertThatThrownBy(() -> productService.changePrice(productId, expected))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @DisplayName("상품의 가격이 변경될 때 메뉴의 가격이 메뉴에 속한 상품 금액의 합보다 크면 메뉴가 숨겨진다.")
  @Test
  void changePriceInMenu() {
    final Product product = productRepository.save(product("후라이드", 16_000L));
    final Menu menu = menuRepository.save(menu(19_000L, true, menuProduct(product, 2L)));
    productService.changePrice(product.getId(), changePriceRequest(8_000L));
    assertThat(menuRepository.findById(menu.getId()).get().isDisplayed()).isFalse();
  }

  @DisplayName("상품의 목록을 조회할 수 있다.")
  @Test
  void findAll() {
    productRepository.save(product("후라이드", 16_000L));
    productRepository.save(product("양념치킨", 16_000L));
    final List<Product> actual = productService.findAll();
    assertThat(actual).hasSize(2);
  }

  private ProductRequest createProductRequest(final String name, final Long price) {
    final ProductRequest product = new ProductRequest();
    product.setName(name);
    product.setPrice(price);
    return product;
  }

  private ProductRequest changePriceRequest(final Long price) {
    final ProductRequest product = new ProductRequest();
    product.setPrice(price);
    return product;
  }

}
