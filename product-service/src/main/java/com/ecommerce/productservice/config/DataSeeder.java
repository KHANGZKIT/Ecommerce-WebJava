package com.ecommerce.productservice.config;

import com.ecommerce.productservice.model.Category;
import com.ecommerce.productservice.model.Product;
import com.ecommerce.productservice.repository.CategoryRepository;
import com.ecommerce.productservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Configuration
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.demo.seed-enabled", havingValue = "true")
public class DataSeeder implements CommandLineRunner {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    @Override
    public void run(String... args) throws Exception {
        log.info("Checking if demo data seeding is needed...");
        seedCategories();
        seedProducts();
        log.info("Demo data check completed.");
    }

    private Category getOrCreateCategory(String name, String description, String imageUrl) {
        return categoryRepository.findByName(name).orElseGet(() -> {
            log.info("Creating category: {}", name);
            return categoryRepository.save(Category.builder()
                    .name(name)
                    .description(description)
                    .imageUrl(imageUrl)
                    .active(true)
                    .build());
        });
    }

    private void seedCategories() {
        getOrCreateCategory("Tai nghe", "Tai nghe chống ồn, gaming, không dây và có dây", "/img/HeadsetM.jpg");
        getOrCreateCategory("Bàn phím cơ", "Bàn phím cơ custom, switch cao cấp và layout công thái học", "/img/NoteBookM.jpg");
        getOrCreateCategory("Chuột máy tính", "Chuột không dây, chuột công thái học cho văn phòng và gaming", "/img/TabletM.jpg");
        getOrCreateCategory("Phụ kiện công nghệ", "Củ sạc GaN, Hub chuyển đổi Type-C, giá đỡ nhôm tản nhiệt", "/img/PhoneM.jpg");
        getOrCreateCategory("Màn hình", "Màn hình đồ họa, gaming, 4K IPS", "/img/watchM.jpg");
        getOrCreateCategory("Laptop", "Laptop mỏng nhẹ, gaming, lập trình và sáng tạo nội dung", "/img/NoteBookM.jpg");
    }

    private void seedProducts() {
        Category audio = categoryRepository.findByName("Tai nghe").orElseThrow();
        Category keyboard = categoryRepository.findByName("Bàn phím cơ").orElseThrow();
        Category mouse = categoryRepository.findByName("Chuột máy tính").orElseThrow();
        Category accessories = categoryRepository.findByName("Phụ kiện công nghệ").orElseThrow();
        Category monitor = categoryRepository.findByName("Màn hình").orElseThrow();
        Category laptop = categoryRepository.findByName("Laptop").orElseThrow();

        List<Product> demoProducts = List.of(
                Product.builder().name("Bàn phím cơ Keychron K8 Pro").sku("KB-KEYCHRON-K8PRO").price(new BigDecimal("2150000")).stockQuantity(50).category(keyboard).imageUrl("https://cdn.cody.mn/img/487376/500x0xwebp/Keyboard_1.jpg").active(true).build(),
                Product.builder().name("Chuột Logitech MX Master 3S").sku("MS-LOGI-MX3S").price(new BigDecimal("2450000")).stockQuantity(30).category(mouse).imageUrl("https://cdn.cody.mn/img/487377/500x0xwebp/Mouse_1.jpg").active(true).build(),
                Product.builder().name("Tai nghe Sony WH-1000XM5").sku("HP-SONY-XM5").price(new BigDecimal("7990000")).stockQuantity(15).category(audio).imageUrl("https://cdn.cody.mn/img/433528/500x0xwebp/HeadsetM.jpg").active(true).build(),
                Product.builder().name("Màn hình Dell UltraSharp 27").sku("MN-DELL-U2723QE").price(new BigDecimal("12500000")).stockQuantity(10).category(monitor).imageUrl("https://cdn.cody.mn/img/433524/500x0xwebp/NoteBookM.jpg").active(true).build(),
                Product.builder().name("Apple MacBook Pro 14 M2").sku("LT-APP-MBP14M2").price(new BigDecimal("45900000")).stockQuantity(8).category(laptop).imageUrl("https://cdn.cody.mn/img/465617/1000x0xwebp/APPLE_MOBILE.png").active(true).build(),
                Product.builder().name("Cáp Sạc Anker PowerLine III").sku("AC-ANKER-PL3").price(new BigDecimal("350000")).stockQuantity(100).category(accessories).imageUrl("https://cdn.cody.mn/img/433525/500x0xwebp/PhoneM.jpg").active(true).build(),
                Product.builder().name("Bàn phím Logitech MX Keys").sku("KB-LOGI-MXKEYS").price(new BigDecimal("2690000")).stockQuantity(40).category(keyboard).imageUrl("https://cdn.cody.mn/img/487378/500x0xwebp/Keyboard_2.jpg").active(true).build(),
                Product.builder().name("Chuột Razer DeathAdder V3 Pro").sku("MS-RAZER-DAV3").price(new BigDecimal("3690000")).stockQuantity(25).category(mouse).imageUrl("https://cdn.cody.mn/img/487379/500x0xwebp/Mouse_2.jpg").active(true).build(),
                Product.builder().name("Tai nghe Apple AirPods Pro 2").sku("HP-APP-APP2").price(new BigDecimal("5990000")).stockQuantity(45).category(audio).imageUrl("https://cdn.cody.mn/img/487253/1920x0xwebp/Air_Pods_5.png").active(true).build(),
                Product.builder().name("Màn hình LG 27UP850N-W").sku("MN-LG-27UP850N").price(new BigDecimal("8990000")).stockQuantity(12).category(monitor).imageUrl("https://cdn.cody.mn/img/433526/500x0xwebp/TabletM.jpg").active(true).build()
        );

        for (Product p : demoProducts) {
            if (!productRepository.existsBySku(p.getSku())) {
                log.info("Seeding product: {}", p.getName());
                productRepository.save(p);
            }
        }
    }
}
