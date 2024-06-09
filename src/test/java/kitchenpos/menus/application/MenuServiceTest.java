package kitchenpos.menus.application;

import static kitchenpos.Fixtures.INVALID_ID;
import static kitchenpos.Fixtures.menu;
import static kitchenpos.Fixtures.menuGroup;
import static kitchenpos.Fixtures.menuProduct;
import static kitchenpos.Fixtures.product;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import kitchenpos.Fixtures;
import kitchenpos.menus.application.dto.MenuRequest;
import kitchenpos.menus.domain.tobe.menu.Menu;
import kitchenpos.menus.domain.tobe.menu.MenuFactory;
import kitchenpos.menus.domain.tobe.menu.ProductClient;
import kitchenpos.menus.domain.tobe.menugroup.MenuGroupRepository;
import kitchenpos.menus.application.dto.MenuProductRequest;
import kitchenpos.menus.domain.tobe.menu.MenuRepository;
import kitchenpos.menus.infra.DefaultProductClient;
import kitchenpos.menus.infra.InMemoryMenuGroupRepository;
import kitchenpos.menus.infra.InMemoryMenuRepository;
import kitchenpos.products.domain.tobe.Product;
import kitchenpos.products.domain.tobe.ProductRepository;
import kitchenpos.common.domain.ProfanityValidator;
import kitchenpos.products.infra.FakeProfanityValidator;
import kitchenpos.products.infra.InMemoryProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class MenuServiceTest {

  private MenuRepository menuRepository;
  private MenuGroupRepository menuGroupRepository;
  private ProductRepository productRepository;
  private ProfanityValidator profanityValidator;
  private ProductClient productClient;
  private MenuService menuService;
  private MenuFactory menuFactory;
  private UUID menuGroupId;
  private Product product;

  private static List<Arguments> menuProducts() {
    return Arrays.asList(
        null,
        Arguments.of(Collections.emptyList()),
        Arguments.of(Arrays.asList(createMenuProductRequest(INVALID_ID, 2L)))
    );
  }

  private static MenuProductRequest createMenuProductRequest(final UUID productId, final long quantity) {
    final MenuProductRequest menuProduct = new MenuProductRequest();
    menuProduct.setProductId(productId);
    menuProduct.setProduct(Fixtures.product());
    menuProduct.setQuantity(quantity);
    return menuProduct;
  }

  @BeforeEach
  void setUp() {
    menuRepository = new InMemoryMenuRepository();
    menuGroupRepository = new InMemoryMenuGroupRepository();
    profanityValidator = new FakeProfanityValidator();
    productRepository = new InMemoryProductRepository();
    productClient = new DefaultProductClient(productRepository);
    menuFactory = new MenuFactory(menuGroupRepository, profanityValidator, productClient);
    menuService = new MenuService(menuRepository, menuFactory);
    menuGroupId = menuGroupRepository.save(menuGroup()).getId();
    product = productRepository.save(product("후라이드", 16_000L));
  }

  @DisplayName("1개 이상의 등록된 상품으로 메뉴를 등록할 수 있다.")
  @Test
  void create() {
    final MenuRequest expected = createMenuRequest(
        "후라이드+후라이드", 19_000L, menuGroupId, true, createMenuProductRequest(product.getId(), 2L)
    );
    final Menu actual = menuService.create(expected);
    assertThat(actual).isNotNull();
    assertAll(
        () -> assertThat(actual.getId()).isNotNull(),
        () -> assertThat(actual.getMenuName()).isEqualTo(expected.getName()),
        () -> assertThat(actual.getMenuPrice()).isEqualTo(expected.getPrice()),
        () -> assertThat(actual.isDisplayed()).isEqualTo(expected.isDisplayed()),
        () -> assertThat(actual.getMenuProducts()).isNotNull()
    );
  }

  @DisplayName("상품이 없으면 등록할 수 없다.")
  @MethodSource("menuProducts")
  @ParameterizedTest
  void create(final List<MenuProductRequest> menuProducts) {
    final MenuRequest expected = createMenuRequest("후라이드+후라이드", 19_000L, menuGroupId, true, menuProducts);
    assertThatThrownBy(() -> menuService.create(expected))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @DisplayName("메뉴에 속한 상품의 수량은 0개 이상이어야 한다.")
  @Test
  void createNegativeQuantity() {
    final MenuRequest expected = createMenuRequest(
        "후라이드+후라이드", 19_000L, menuGroupId, true, createMenuProductRequest(product.getId(), -1L)
    );
    assertThatThrownBy(() -> menuService.create(expected))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @DisplayName("메뉴의 가격이 올바르지 않으면 등록할 수 없다.")
  @ValueSource(strings = "-1000")
  @NullSource
  @ParameterizedTest
  void create(final BigDecimal price) {
    final MenuRequest expected = createMenuRequest(
        "후라이드+후라이드", price, menuGroupId, true, createMenuProductRequest(product.getId(), 2L)
    );
    assertThatThrownBy(() -> menuService.create(expected))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @DisplayName("메뉴에 속한 상품 금액의 합은 메뉴의 가격보다 크거나 같아야 한다.")
  @Test
  void createExpensiveMenu() {
    final MenuRequest expected = createMenuRequest(
        "후라이드+후라이드", 33_000L, menuGroupId, true, createMenuProductRequest(product.getId(), 2L)
    );
    assertThatThrownBy(() -> menuService.create(expected))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @DisplayName("메뉴는 특정 메뉴 그룹에 속해야 한다.")
  @NullSource
  @ParameterizedTest
  void create(final UUID menuGroupId) {
    final MenuRequest expected = createMenuRequest(
        "후라이드+후라이드", 19_000L, menuGroupId, true, createMenuProductRequest(product.getId(), 2L)
    );
    assertThatThrownBy(() -> menuService.create(expected))
        .isInstanceOf(NoSuchElementException.class);
  }

  @DisplayName("메뉴의 이름이 올바르지 않으면 등록할 수 없다.")
  @ValueSource(strings = {"비속어", "욕설이 포함된 이름"})
  @NullSource
  @ParameterizedTest
  void create(final String name) {
    final MenuRequest expected = createMenuRequest(
        name, 19_000L, menuGroupId, true, createMenuProductRequest(product.getId(), 2L)
    );
    assertThatThrownBy(() -> menuService.create(expected))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @DisplayName("메뉴의 가격을 변경할 수 있다.")
  @Test
  void changePrice() {
    final UUID menuId = menuRepository.save(menu(19_000L, true,menuProduct(product, 2L))).getId();
    final MenuRequest expected = changePriceRequest(16_000L);
    final Menu actual = menuService.changePrice(menuId, expected);
    assertThat(actual.getMenuPrice()).isEqualTo(expected.getPrice());
  }

  @DisplayName("메뉴의 가격이 올바르지 않으면 변경할 수 없다.")
  @ValueSource(strings = "-1000")
  @NullSource
  @ParameterizedTest
  void changePrice(final BigDecimal price) {
    final UUID menuId = menuRepository.save(menu(19_000L, true, menuProduct(product, 2L))).getId();
    final MenuRequest expected = changePriceRequest(price);
    assertThatThrownBy(() -> menuService.changePrice(menuId, expected))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @DisplayName("메뉴에 속한 상품 금액의 합은 메뉴의 가격보다 크거나 같아야 한다.")
  @Test
  void changePriceToExpensive() {
    final UUID menuId = menuRepository.save(menu(19_000L, true, menuProduct(product, 2L))).getId();
    final MenuRequest expected = changePriceRequest(33_000L);
    assertThatThrownBy(() -> menuService.changePrice(menuId, expected))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @DisplayName("메뉴를 노출할 수 있다.")
  @Test
  void display() {
    final UUID menuId = menuRepository.save(menu(19_000L, false, menuProduct(product, 2L))).getId();
    final Menu actual = menuService.display(menuId);
    assertThat(actual.isDisplayed()).isTrue();
  }

  @DisplayName("메뉴의 가격이 메뉴에 속한 상품 금액의 합보다 높을 경우 메뉴를 노출할 수 없다.")
  @Test
  void displayExpensiveMenu() {
    assertThatThrownBy(() -> menuRepository.save(menu(33_000L, true, menuProduct(product, 2L))))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @DisplayName("메뉴를 숨길 수 있다.")
  @Test
  void hide() {
    final UUID menuId = menuRepository.save(menu(19_000L, true, menuProduct(product, 2L))).getId();
    final Menu actual = menuService.hide(menuId);
    assertThat(actual.isDisplayed()).isFalse();
  }

  @DisplayName("메뉴의 목록을 조회할 수 있다.")
  @Test
  void findAll() {
    menuRepository.save(menu(19_000L, true, menuProduct(product, 2L)));
    final List<Menu> actual = menuService.findAll();
    assertThat(actual).hasSize(1);
  }

  private MenuRequest createMenuRequest(
      final String name,
      final long price,
      final UUID menuGroupId,
      final boolean displayed,
      final MenuProductRequest... menuProducts
  ) {
    return createMenuRequest(name, BigDecimal.valueOf(price), menuGroupId, displayed, menuProducts);
  }

  private MenuRequest createMenuRequest(
      final String name,
      final BigDecimal price,
      final UUID menuGroupId,
      final boolean displayed,
      final MenuProductRequest... menuProducts
  ) {
    return createMenuRequest(name, price, menuGroupId, displayed, Arrays.asList(menuProducts));
  }

  private MenuRequest createMenuRequest(
      final String name,
      final long price,
      final UUID menuGroupId,
      final boolean displayed,
      final List<MenuProductRequest> menuProducts
  ) {
    return createMenuRequest(name, BigDecimal.valueOf(price), menuGroupId, displayed, menuProducts);
  }

  private MenuRequest createMenuRequest(
      final String name,
      final BigDecimal price,
      final UUID menuGroupId,
      final boolean displayed,
      final List<MenuProductRequest> menuProducts
  ) {
    final MenuRequest menu = new MenuRequest();
    menu.setName(name);
    menu.setPrice(price);
    menu.setMenuGroupId(menuGroupId);
    menu.setDisplayed(displayed);
    menu.setMenuProducts(menuProducts);
    return menu;
  }

  private MenuRequest changePriceRequest(final long price) {
    return changePriceRequest(BigDecimal.valueOf(price));
  }

  private MenuRequest changePriceRequest(final BigDecimal price) {
    final MenuRequest menu = new MenuRequest();
    menu.setPrice(price);
    return menu;
  }
}
