package com.veterinaire.formulaireveterinaire.serviceimpl;
import com.veterinaire.formulaireveterinaire.DAO.ProductRepository;
import com.veterinaire.formulaireveterinaire.DAO.ProductVariantRepository;
import com.veterinaire.formulaireveterinaire.Enums.SubCategory;
import com.veterinaire.formulaireveterinaire.entity.Product;
import com.veterinaire.formulaireveterinaire.entity.ProductVariant;
import com.veterinaire.formulaireveterinaire.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
@Service
public class ProductServiceImpl implements ProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductServiceImpl.class);

    @Autowired
    private ProductRepository productRepository;

     // for variant updates

    @Autowired
    private ProductVariantRepository variantRepository;

    public List<Product> getAllProducts() {
        return productRepository.findAllWithVariants();
    }

    @Override
    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }

    @Override
    public Product createProduct(Product product) {
        validateSubSubCategoryRules(product);

        // Existing image scraping logic (unchanged)
        if (product.getDetailsUrl() != null && (product.getImageUrl() == null || product.getImageUrl().isEmpty())) {
            try {
                String scrapedImageUrl = scrapeProductImage(product.getDetailsUrl());
                if (scrapedImageUrl != null) {
                    product.setImageUrl(scrapedImageUrl);
                    logger.info("Successfully scraped image URL: {}", scrapedImageUrl);
                } else {
                    logger.warn("No image found on detailsUrl: {}", product.getDetailsUrl());
                    product.setImageUrl("https://via.placeholder.com/300x300?text=No+Image");
                }
            } catch (IOException e) {
                logger.error("Error scraping image from {}: {}", product.getDetailsUrl(), e.getMessage());
                product.setImageUrl("https://via.placeholder.com/300x300?text=Error+Loading");
            }
        }

        return productRepository.save(product);
    }



    @Override
    public Product updateProductWithVariants(Long id, Product product) {
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));

        validateSubSubCategoryRules(product);

        // update product fields
        existing.setName(product.getName());
        existing.setDescription(product.getDescription());
        existing.setImageUrl(product.getImageUrl());
        existing.setCategory(product.getCategory());
        existing.setSubCategory(product.getSubCategory());
        existing.setSubSubCategory(product.getSubSubCategory());
        existing.setInStock(product.getInStock());
        existing.setDetailsUrl(product.getDetailsUrl());

        // update variants if provided
        if (product.getVariants() != null && !product.getVariants().isEmpty()) {
            for (ProductVariant incoming : product.getVariants()) {
                if (incoming.getId() != null) {
                    // update existing variant
                    ProductVariant existingVariant = variantRepository.findById(incoming.getId())
                            .orElseThrow(() -> new RuntimeException("Variant not found with id: " + incoming.getId()));
                    existingVariant.setPackaging(incoming.getPackaging());
                    existingVariant.setPrice(incoming.getPrice());
                    variantRepository.save(existingVariant);
                } else {
                    // add new variant
                    incoming.setProduct(existing);
                    variantRepository.save(incoming);
                }
            }
        }

        return productRepository.save(existing);
    }

    /**
     * Central validation rule for subCategory + subSubCategory combination
     */
    private void validateSubSubCategoryRules(Product product) {
        if (product.getSubCategory() == null) {
            throw new IllegalArgumentException("La sous-catégorie (subCategory) est obligatoire");
        }

        if (product.getSubCategory() == SubCategory.ALIMENT) {
            if (product.getSubSubCategory() == null) {
                throw new IllegalArgumentException(
                        "Pour la sous-catégorie ALIMENT, vous devez obligatoirement choisir une sous-sous-catégorie : DIETETIQUE ou physiologique"
                );
            }
        } else {
            // For TEST_RAPIDE and COMPLEMENT → subSubCategory must not be set
            if (product.getSubSubCategory() != null) {
                logger.info("subSubCategory ignorée car subCategory = {} (non ALIMENT)", product.getSubCategory());
                product.setSubSubCategory(null); // force clear
            }
        }
    }

    private String scrapeProductImage(String url) throws IOException {
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .timeout(10000) // 10-second timeout
                    .get();

            // Prioritize product images within figure or anchor tags
            Elements images = doc.select("figure img, a img[src*='files/live/sites/virbac-tn/files']");

            // Broader selectors if specific fails
            if (images.isEmpty()) {
                images = doc.select("img[src*='files/live/sites/virbac-tn/files']");
            }
            if (images.isEmpty()) {
                images = doc.select("img[src*='packshot'], img[class*='packshot'], img[alt*='product']");
            }
            if (images.isEmpty()) {
                images = doc.select("img[src*='product'], .product-image img, .main-image img, img[itemprop='image']");
            }
            if (images.isEmpty()) {
                images = doc.select("img[width>=200], img[height>=200]");
            }
            if (images.isEmpty()) {
                images = doc.select("img");
            }

            // Debug: Log all found images
            logger.debug("Found {} potential images on {}", images.size(), url);
            for (Element img : images) {
                String src = img.attr("src");
                String dataSrc = img.attr("data-src");
                if (!src.isEmpty()) {
                    logger.debug("Candidate image src: {}", src);
                } else if (!dataSrc.isEmpty()) {
                    logger.debug("Candidate image data-src: {}", dataSrc);
                }
                if (src.contains("files/live/sites/virbac-tn/files") || dataSrc.contains("files/live/sites/virbac-tn/files")) {
                    logger.debug("Prioritized image candidate: {}", src.isEmpty() ? dataSrc : src);
                }
            }

            if (!images.isEmpty()) {
                Element firstImage = null;
                // Prioritize an image with Virbac file path
                for (Element img : images) {
                    String src = img.attr("src");
                    String dataSrc = img.attr("data-src");
                    if (!src.isEmpty() && src.contains("files/live/sites/virbac-tn/files")) {
                        firstImage = img;
                        break;
                    } else if (src.isEmpty() && !dataSrc.isEmpty() && dataSrc.contains("files/live/sites/virbac-tn/files")) {
                        firstImage = img;
                        src = dataSrc; // Use data-src if src is empty
                        break;
                    }
                }
                // Fallback to first image if no prioritized one
                if (firstImage == null) {
                    firstImage = images.first();
                }

                String src = firstImage.attr("src").isEmpty() ? firstImage.attr("data-src") : firstImage.attr("src");
                // Make absolute URL
                if (src.startsWith("/")) {
                    src = "https://tn.virbac.com" + src;
                } else if (!src.startsWith("http")) {
                    String base = url.substring(0, url.lastIndexOf("/") + 1);
                    src = base + src;
                }
                // Verify it's an image URL
                if (!src.endsWith("/") && (src.endsWith(".png") || src.endsWith(".jpg"))) {
                    logger.debug("Selected scraped image URL: {}", src);
                    return src;
                } else {
                    logger.warn("Selected URL {} is not a valid image, skipping", src);
                    return null;
                }
            }

            logger.warn("No suitable images found on page: {}", url);
            return null;
        } catch (Exception e) {
            logger.error("Failed to scrape image from {}: {}", url, e.getMessage());
            throw new IOException("Failed to scrape image", e);
        }
    }

    @Override
    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }

    @Override
    public List<Product> getProductsByCategory(String category) {
        return productRepository.findByCategory(category);
    }

    @Override
    public List<Product> getProductsBySubCategory(String subCategory) {
        return productRepository.findBySubCategory(subCategory);
    }

    @Override
    public List<Product> getProductsByStockStatus(Boolean inStock) {
        return productRepository.findByInStock(inStock);
    }

    @Override
    public ProductVariant addVariant(Long productId, ProductVariant variant) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));
        variant.setProduct(product);
        return variantRepository.save(variant);
    }

    @Override
    public void deleteVariant(Long variantId) {
        variantRepository.deleteById(variantId);
    }

    @Override
    public List<ProductVariant> getVariantsByProduct(Long productId) {
        return variantRepository.findByProductId(productId);
}
}