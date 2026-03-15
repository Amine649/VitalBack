package com.veterinaire.formulaireveterinaire.serviceimpl;

import com.veterinaire.formulaireveterinaire.DAO.OurVeterinaireRepository;
import com.veterinaire.formulaireveterinaire.DAO.ProductRepository;
import com.veterinaire.formulaireveterinaire.DAO.ProductVariantRepository;
import com.veterinaire.formulaireveterinaire.DAO.UserRepository;
import com.veterinaire.formulaireveterinaire.DTO.Cart.CartItemDto;
import com.veterinaire.formulaireveterinaire.DTO.Cart.CartItemRequest;
import com.veterinaire.formulaireveterinaire.DTO.Cart.CartResponse;
import com.veterinaire.formulaireveterinaire.DTO.Cart.CheckoutRequest;
import com.veterinaire.formulaireveterinaire.entity.*;
import com.veterinaire.formulaireveterinaire.Enums.OrderStatus;
import com.veterinaire.formulaireveterinaire.DAO.Cart.CartOrderRepository;
import com.veterinaire.formulaireveterinaire.DAO.Cart.OrderItemRepository;
import com.veterinaire.formulaireveterinaire.service.CartService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartOrderRepository cartOrderRepo;
    private final OrderItemRepository itemRepo;
    private final UserRepository userRepo;
    private final ProductRepository productRepo;
    private final JavaMailSender mailSender;
    private final OurVeterinaireRepository ourVeterinaireRepository;

    @Autowired
    private ProductVariantRepository variantRepo;

    @Value("${finance.email}")
    private String financeEmail;

    private static final Logger logger = LoggerFactory.getLogger(CartServiceImpl.class);

    // --- Helper: Get or create CART ---
    private CartOrder getOrCreateCart(Long userId) {
        return cartOrderRepo.findByUserIdAndStatus(userId, OrderStatus.CART)
                .orElseGet(() -> {
                    CartOrder cart = new CartOrder();
                    cart.setUserId(userId);
                    cart.setStatus(OrderStatus.CART);
                    cart.setTotalAmount(BigDecimal.ZERO);
                    return cartOrderRepo.save(cart);
                });
    }

    // --- Recalculate total ---
    private void recalcTotal(Long orderId) {
        List<OrderItem> items = itemRepo.findByOrderId(orderId);
        BigDecimal total = items.stream()
                .map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        CartOrder order = (CartOrder) cartOrderRepo.findById(orderId).orElseThrow();
        order.setTotalAmount(total);
        cartOrderRepo.save(order);
    }

        @Override
        public CartResponse getCart(Long userId) {
            CartOrder cart = getOrCreateCart(userId);
            List<OrderItem> items = itemRepo.findByOrderId(cart.getId());

            List<CartItemDto> dtos = items.stream().map(item -> {
                Product product = productRepo.findById(item.getProductId())
                        .orElseThrow(() -> new EntityNotFoundException("Product not found: " + item.getProductId()));

                CartItemDto dto = new CartItemDto();
                dto.setItemId(item.getId());
                dto.setProductId(item.getProductId());
                dto.setProductName(product.getName());
                dto.setImageUrl(product.getImageUrl());
                dto.setQuantity(item.getQuantity());
                dto.setPrice(item.getPrice());
                dto.setSubTotal(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));

                // ✅ add variant info
                if (item.getVariantId() != null) {
                    variantRepo.findById(item.getVariantId()).ifPresent(variant -> {
                        dto.setVariantId(variant.getId());
                        dto.setPackaging(variant.getPackaging());
                    });
                }

                return dto;
            }).toList();

            CartResponse resp = new CartResponse();
            resp.setCartId(cart.getId());
            resp.setTotalAmount(cart.getTotalAmount());
            resp.setItems(dtos);
            return resp;
        }

    @Override
    @Transactional
    public CartItemDto addItem(Long userId, CartItemRequest req) {

        CartOrder cart = getOrCreateCart(userId);

        Product product = productRepo.findById(req.getProductId())
                .orElseThrow(() -> new EntityNotFoundException("Product not found"));

        ProductVariant variant = variantRepo.findById(req.getVariantId())
                .orElseThrow(() -> new EntityNotFoundException("Variant not found"));

        BigDecimal price = variant.getPrice();

        OrderItem item = new OrderItem();
        item.setOrderId(cart.getId());
        item.setProductId(product.getId());
        item.setVariantId(variant.getId());
        item.setQuantity(req.getQuantity());
        item.setPrice(price);

        item = itemRepo.save(item);

        recalcTotal(cart.getId());

        CartItemDto dto = new CartItemDto();
        dto.setItemId(item.getId());
        dto.setProductId(product.getId());
        dto.setProductName(product.getName());
        dto.setImageUrl(product.getImageUrl());
        dto.setVariantId(variant.getId());
        dto.setPackaging(variant.getPackaging());
        dto.setQuantity(item.getQuantity());
        dto.setPrice(item.getPrice());
        dto.setSubTotal(price.multiply(BigDecimal.valueOf(item.getQuantity())));

        return dto;
    }

//    @Override
//    @Transactional
//    public CartItemDto addItem(Long userId, CartItemRequest req) {
//        CartOrder cart = getOrCreateCart(userId);
//
//        Product product = productRepo.findById(req.getProductId())
//                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + req.getProductId()));
//
//        // ✅ get first variant price automatically
//        List<ProductVariant> variants = variantRepo.findByProductId(req.getProductId());
//
//        if (variants.isEmpty()) {
//            throw new IllegalArgumentException("Product has no variants configured");
//        }
//
//        ProductVariant defaultVariant = variants.get(0); // first variant as default
//        BigDecimal price = defaultVariant.getPrice();
//
//        OrderItem item = new OrderItem();
//        item.setOrderId(cart.getId());
//        item.setProductId(req.getProductId());
//        item.setVariantId(defaultVariant.getId()); // ✅ store default variant
//        item.setQuantity(req.getQuantity());
//        item.setPrice(price);
//        item = itemRepo.save(item);
//
//        recalcTotal(cart.getId());
//
//        CartItemDto dto = new CartItemDto();
//        dto.setItemId(item.getId());
//        dto.setProductId(item.getProductId());
//        dto.setProductName(product.getName());
//        dto.setImageUrl(product.getImageUrl());
//        dto.setVariantId(defaultVariant.getId());
//        dto.setPackaging(defaultVariant.getPackaging()); // ✅ show which packaging was selected
//        dto.setQuantity(item.getQuantity());
//        dto.setPrice(item.getPrice());
//        dto.setSubTotal(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
//        return dto;
//    }

    @Override
    @Transactional
    public CartItemDto updateItem(Long userId, Long itemId, CartItemRequest req) {
        CartOrder cart = getOrCreateCart(userId);
        OrderItem item = itemRepo.findById(itemId)
                .orElseThrow(() -> new EntityNotFoundException("Cart item not found"));

        if (!item.getOrderId().equals(cart.getId())) {
            throw new IllegalArgumentException("Item does not belong to this user's cart");
        }

        if (req.getQuantity() <= 0) {
            itemRepo.delete(item);
            recalcTotal(cart.getId());
            return null;
        }

        // ✅ update variant and price if variantId provided
        if (req.getVariantId() != null) {
            ProductVariant variant = variantRepo.findById(req.getVariantId())
                    .orElseThrow(() -> new EntityNotFoundException("Variant not found: " + req.getVariantId()));

            if (!variant.getProduct().getId().equals(item.getProductId())) {
                throw new IllegalArgumentException("Variant does not belong to this product");
            }

            item.setVariantId(variant.getId());
            item.setPrice(variant.getPrice()); // ✅ price updated
        }

        item.setQuantity(req.getQuantity());
        itemRepo.save(item);
        recalcTotal(cart.getId());

        Product product = productRepo.findById(item.getProductId())
                .orElseThrow(() -> new EntityNotFoundException("Product not found"));

        ProductVariant currentVariant = variantRepo.findById(item.getVariantId())
                .orElseThrow(() -> new EntityNotFoundException("Variant not found"));

        CartItemDto dto = new CartItemDto();
        dto.setItemId(item.getId());
        dto.setProductId(item.getProductId());
        dto.setProductName(product.getName());
        dto.setImageUrl(product.getImageUrl());
        dto.setVariantId(item.getVariantId());
        dto.setPackaging(currentVariant.getPackaging());
        dto.setQuantity(item.getQuantity());
        dto.setPrice(item.getPrice());
        dto.setSubTotal(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        return dto;
    }

    @Override
    @Transactional
    public void removeItem(Long userId, Long itemId) {
        CartOrder cart = getOrCreateCart(userId);
        OrderItem item = itemRepo.findById(itemId)
                .orElseThrow(() -> new EntityNotFoundException("Item not found"));

        if (!item.getOrderId().equals(cart.getId())) {
            throw new IllegalArgumentException("Item does not belong to user's cart");
        }

        itemRepo.delete(item);
        recalcTotal(cart.getId());
    }

    @Override
    @Transactional
    public void clearCart(Long userId) {
        CartOrder cart = getOrCreateCart(userId);
        itemRepo.deleteByOrderId(cart.getId());
        cart.setTotalAmount(BigDecimal.ZERO);
        cartOrderRepo.save(cart);
    }

    @Override
    @Transactional
    public String checkout(Long userId, CheckoutRequest request) {
        CartOrder cart = cartOrderRepo.findByUserIdAndStatus(userId, OrderStatus.CART)
                .orElseThrow(() -> new EntityNotFoundException("No cart found"));

        if (cart.getTotalAmount() == null || cart.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Cannot checkout empty cart");
        }

        // Confirm order
        cart.setStatus(OrderStatus.CONFIRMED);
        String orderNumber = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        cart.setOrderNumber(orderNumber);
        cart.setConfirmedAt(LocalDateTime.now());
        cartOrderRepo.save(cart);

        // Fetch user
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        // Use the email from request if provided, otherwise use user's email
        String emailToUse = (request.getDeliveryAddress() != null && !request.getDeliveryAddress().isEmpty())
                ? request.getDeliveryAddress()
                : user.getEmail();

        // SEND EMAIL with the provided email
        sendOrderConfirmationEmail(user, cart, emailToUse, financeEmail);

        return orderNumber;
    }

    private void sendOrderConfirmationEmail(User user, CartOrder order, String ccEmail, String financeEmail) {
        MimeMessage message = mailSender.createMimeMessage();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String orderDateStr = order.getConfirmedAt().format(formatter);

        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(user.getEmail());
            helper.setCc(ccEmail);
            helper.setSubject("Commande Confirmée – VITALFEED");

            String nom = user.getNom() != null ? user.getNom() : "Cher client";

            List<OrderItem> items = itemRepo.findByOrderId(order.getId());
            StringBuilder itemsHtml = new StringBuilder();

            for (OrderItem item : items) {
                Product product = productRepo.findById(item.getProductId())
                        .orElseThrow(() -> new EntityNotFoundException("Product not found"));

                // ✅ fetch variant info
                String packaging = "";
                if (item.getVariantId() != null) {
                    packaging = variantRepo.findById(item.getVariantId())
                            .map(ProductVariant::getPackaging)
                            .orElse("");
                }

                String productName = product.getName();
                String imageUrl = product.getImageUrl() != null ? product.getImageUrl() : "";
                BigDecimal subTotal = item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                String unitPriceStr = String.format("%.2f", item.getPrice());
                String subTotalStr = String.format("%.2f", subTotal);

                itemsHtml.append("""
                <tr style="border-bottom:1px solid #eee;">
                    <td style="padding:12px; vertical-align:middle;">
                        <img src="%s" alt="%s" style="width:60px; height:60px; object-fit:cover; border-radius:6px; float:left; margin-right:12px;">
                        <div style="margin-left:72px;">
                            <strong style="font-size:15px;">%s</strong>
                        </div>
                    </td>
                    <td style="padding:12px; text-align:center; vertical-align:middle;">%s</td>
                    <td style="padding:12px; text-align:center; vertical-align:middle; font-weight:600;">%d</td>
                    <td style="padding:12px; text-align:right; vertical-align:middle; font-weight:600;">%s TND</td>
                    <td style="padding:12px; text-align:right; vertical-align:middle; font-weight:600;">%s TND</td>
                </tr>
                """.formatted(
                        imageUrl,
                        productName,
                        productName,
                        packaging,
                        item.getQuantity(),
                        unitPriceStr,
                        subTotalStr
                ));
            }

            String totalStr = String.format("%.2f", order.getTotalAmount());

            String htmlContent = """
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <title>Commande Confirmée – VITALFEED</title>
</head>
<body style="margin:0; padding:0; background-color:#f7f9fc; font-family:Segoe UI, Tahoma, Geneva, Verdana, sans-serif; color:#333;">
  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0">
    <tr>
      <td align="center" style="padding:30px 0;">
        <table width="680" cellpadding="0" cellspacing="0" border="0" style="background:#ffffff; border-radius:12px; overflow:hidden;">
          <!-- Header -->
          <tr>
            <td align="center" style="background-color:#00897B; color:#fff; padding:30px;">
              <h1 style="margin:0; font-size:26px;">VITALFEED</h1>
              <p style="margin:8px 0 0; font-size:14px;">Simplifier Votre Quotidien Professionnel</p>
            </td>
          </tr>

          <!-- Content -->
          <tr>
            <td style="padding:40px;">
              <p style="font-size:16px; line-height:1.6; margin-bottom:25px;">
                <strong>Bonjour Dr %s,</strong><br>
                Votre commande sur <strong>VITALFEED</strong> a été <strong>confirmée avec succès</strong>.<br>
              </p>

              <h3 style="color:#00897B; border-bottom:2px solid #e0f2f1; padding-bottom:6px; font-size:18px;">Détails de la commande</h3>
              <table width="100%%" cellspacing="0" cellpadding="4" style="font-size:15px; margin-bottom:30px;">
                <tr>
                  <td><strong>Numéro de commande :</strong></td>
                  <td align="right">%s</td>
                </tr>
                <tr>
                  <td><strong>Date de confirmation :</strong></td>
                  <td align="right">%s</td>
                </tr>
              </table>

              <h3 style="color:#00897B; border-bottom:2px solid #e0f2f1; padding-bottom:6px; font-size:18px;">Produits commandés</h3>
              <table width="100%%" cellpadding="0" cellspacing="0" border="0" style="border-collapse:collapse; font-size:15px;">
                <thead>
                  <tr style="background:#e0f2f1; color:#00695c;">
                    <th align="left" style="padding:12px;">Produit</th>
                    <th align="center" style="padding:12px;">Emballage</th>
                    <th align="center" style="padding:12px;">Quantité</th>
                    <th align="right" style="padding:12px;">Prix unitaire</th>
                    <th align="right" style="padding:12px;">Sous-total</th>
                  </tr>
                </thead>
                <tbody>
                  %s
                  <tr style="background:#e8f5e9; font-weight:700;">
                    <td colspan="4" align="right" style="padding:15px;">Total :</td>
                    <td align="right" style="padding:15px;">%s TND</td>
                  </tr>
                </tbody>
              </table>

              <p style="text-align:center; margin-top:35px; font-size:15px; color:#555;">Merci pour votre confiance !</p>
              <p style="text-align:center; margin:20px 0 5px; font-weight:600;">Bien cordialement,</p>
              <p style="text-align:center; margin:0; color:#00897B; font-weight:700;">L'équipe VITALFEED</p>
            </td>
          </tr>

          <!-- Footer -->
          <tr>
            <td align="center" style="background:#f0f4f8; padding:20px; font-size:13px; color:#666;">
              <p style="margin:0;">Cet e-mail a été envoyé automatiquement, merci de ne pas y répondre.</p>
              <p style="margin:5px 0 0;">© %s VITALFEED – Tous droits réservés.</p>
            </td>
          </tr>
        </table>
      </td>
    </tr>
  </table>
</body>
</html>
""".formatted(
                    nom,
                    order.getOrderNumber(),
                    orderDateStr,
                    itemsHtml.toString(),
                    totalStr,
                    String.valueOf(LocalDate.now().getYear())
            );

            helper.setText(htmlContent, true);
            mailSender.send(message);

            logger.info("Order confirmation email sent to {} with CC to {}", user.getEmail(), ccEmail);

        } catch (MessagingException e) {
            logger.error("Failed to send order email: {}", e.getMessage());
            throw new RuntimeException("Erreur lors de l'envoi de l'email", e);
        }
    }


}
