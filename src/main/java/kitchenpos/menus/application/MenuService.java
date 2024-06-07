package kitchenpos.menus.application;

import kitchenpos.common.domain.ProductPriceChangeEvent;
import kitchenpos.common.domain.ProfanityValidator;
import kitchenpos.menus.application.dto.MenuRequest;
import kitchenpos.menus.domain.tobe.menu.*;
import kitchenpos.menus.domain.tobe.menugroup.MenuGroupRepository;
import kitchenpos.products.domain.tobe.ProductRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class MenuService {
    private final MenuRepository menuRepository;

    private final MenuDomainService menuDomainService;

    public MenuService(
        final MenuRepository menuRepository,
        final MenuGroupRepository menuGroupRepository,
        final ProductRepository productRepository,
        final ProfanityValidator profanityValidator,
        MenuDomainService menuDomainService) {
        this.menuRepository = menuRepository;
        this.menuDomainService = menuDomainService;
    }

    @Transactional
    public Menu create(final MenuRequest request) {
        final Menu menu = menuDomainService.createMenu(request.getMenuGroupId(), request.getMenuProducts(), request.isDisplayed(), request.getName(), request.getPrice());
        return menuRepository.save(menu);
    }

    @Transactional
    public Menu changePrice(final UUID menuId, final MenuRequest request) {
        final Menu menu = menuRepository.findById(menuId)
            .orElseThrow(NoSuchElementException::new);

        menu.changePrice(request.getPrice());
        return menu;
    }

    @TransactionalEventListener
    @Async
    public void changeMenuProdutPrice(final ProductPriceChangeEvent productPriceChangeEvent) {
        final List<Menu> menus = menuRepository.findAllByProductId(productPriceChangeEvent.getProductId());

        for (final Menu menu : menus) {
            menu.getMenuProducts().changeMenuProductPrice(productPriceChangeEvent.getProductId(), productPriceChangeEvent.getPrice());

            if (menu.getMenuPrice().compareTo(menu.getMenuProducts().sum()) > 0) {
                menu.hide();
            }
        }
    }


    @Transactional
    public Menu display(final UUID menuId) {
        final Menu menu = menuRepository.findById(menuId)
            .orElseThrow(NoSuchElementException::new);
        menu.display();
        return menu;
    }

    @Transactional
    public Menu hide(final UUID menuId) {
        final Menu menu = menuRepository.findById(menuId)
            .orElseThrow(NoSuchElementException::new);
        menu.hide();
        return menu;
    }

    @Transactional(readOnly = true)
    public List<Menu> findAll() {
        return menuRepository.findAll();
    }
}
