package com.veterinaire.formulaireveterinaire.serviceimpl;

import com.veterinaire.formulaireveterinaire.DAO.Cart.CartOrderRepository;
import com.veterinaire.formulaireveterinaire.DAO.ProductVariantRepository;
import com.veterinaire.formulaireveterinaire.DTO.Cart.CartItemDto;
import com.veterinaire.formulaireveterinaire.DTO.Cart.CommercialCartItemRequest;
import com.veterinaire.formulaireveterinaire.DTO.Cart.CommercialCartResponse;
import com.veterinaire.formulaireveterinaire.DTO.Cart.CommercialOrderConfirmationResponse;
import com.veterinaire.formulaireveterinaire.Enums.OrderStatus;
import com.veterinaire.formulaireveterinaire.entity.*;
import com.veterinaire.formulaireveterinaire.service.CommercialOrderService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import com.veterinaire.formulaireveterinaire.DAO.Cart.OrderItemRepository;
import com.veterinaire.formulaireveterinaire.DAO.OurVeterinaireRepository;
import com.veterinaire.formulaireveterinaire.DAO.ProductRepository;
import com.veterinaire.formulaireveterinaire.DAO.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommercialOrderServiceImpl implements CommercialOrderService {

    private final CartOrderRepository cartOrderRepo;
    private final OrderItemRepository itemRepo;
    private final UserRepository userRepo;
    private final ProductRepository productRepo;
    private final OurVeterinaireRepository ourVeterinaireRepository;
    private final JavaMailSender mailSender;


    @Autowired
    private ProductVariantRepository variantRepo;


    @Value("${finance.email}")
    private String financeEmail;


    private static final Logger log = LoggerFactory.getLogger(CommercialOrderServiceImpl.class);



    public CartOrder loadOrCreateCommercialCart(String vetMatricule) {
        return cartOrderRepo.findByVetMatriculeAndStatusAndOrderedByCommercial(
                        vetMatricule, OrderStatus.CART, true)
                .orElseGet(() -> {
                    ourVeterinaireRepository.findByMatricule(vetMatricule)
                            .orElseThrow(() -> new EntityNotFoundException(
                                    "Vétérinaire non trouvé avec matricule : " + vetMatricule));

                    CartOrder newCart = new CartOrder();
                    newCart.setUserId(null);
                    newCart.setVetMatricule(vetMatricule);
                    newCart.setOrderedByCommercial(true);
                    newCart.setStatus(OrderStatus.CART);
                    newCart.setTotalAmount(BigDecimal.ZERO);

                    return cartOrderRepo.save(newCart);   // now allowed
                });
    }



    @Override
    public CommercialCartResponse getOrCreateCommercialCart(String vetMatricule) {
        CartOrder cart = loadOrCreateCommercialCart(vetMatricule);

        List<CartItemDto> items = itemRepo.findByOrderId(cart.getId()).stream()
                .map(item -> {
                    Product p = productRepo.findById(item.getProductId())
                            .orElseThrow(() -> new EntityNotFoundException("Produit introuvable"));

                    CartItemDto dto = new CartItemDto();
                    dto.setItemId(item.getId());
                    dto.setProductId(item.getProductId());
                    dto.setProductName(p.getName());
                    dto.setImageUrl(p.getImageUrl());
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
                })
                .toList();

        OurVeterinaire vet = ourVeterinaireRepository
                .findByMatricule(vetMatricule)
                .orElseThrow();

        CommercialCartResponse resp = new CommercialCartResponse();
        resp.setCartId(cart.getId());
        resp.setVetMatricule(vetMatricule);
        resp.setVetFullName(vet.getNom() + " " );
        resp.setTotalAmount(cart.getTotalAmount());
        resp.setItems(items);

        return resp;
    }


    @Override
    @Transactional
    public CartItemDto addItemToCommercialCart(String vetMatricule, CommercialCartItemRequest request, Long commercialUserId) {
        CartOrder cart = loadOrCreateCommercialCart(vetMatricule);

        Product product = productRepo.findById(request.getProductId())
                .orElseThrow(() -> new EntityNotFoundException("Produit introuvable"));

        List<ProductVariant> variants = variantRepo.findByProductId(request.getProductId());
        if (variants.isEmpty()) {
            throw new IllegalArgumentException("Product has no variants configured");
        }
        ProductVariant defaultVariant = variants.get(0);

        // ✅ match by productId AND variantId — allows same product different variant
        Optional<OrderItem> existingOpt = itemRepo.findByOrderIdAndProductIdAndVariantId(
                cart.getId(),
                request.getProductId(),
                defaultVariant.getId()
        );

        OrderItem item;
        int quantityToAdd = request.getQuantity();

        if (existingOpt.isPresent()) {
            item = existingOpt.get();
            item.setQuantity(item.getQuantity() + quantityToAdd);
        } else {
            item = new OrderItem();
            item.setOrderId(cart.getId());
            item.setProductId(request.getProductId());
            item.setVariantId(defaultVariant.getId());
            item.setPrice(defaultVariant.getPrice());
            item.setQuantity(quantityToAdd);
        }

        itemRepo.save(item);
        recalculateTotal(cart.getId());

        CartItemDto dto = new CartItemDto();
        dto.setItemId(item.getId());
        dto.setProductId(item.getProductId());
        dto.setProductName(product.getName());
        dto.setImageUrl(product.getImageUrl());
        dto.setVariantId(defaultVariant.getId());
        dto.setPackaging(defaultVariant.getPackaging());
        dto.setQuantity(item.getQuantity());
        dto.setPrice(item.getPrice());
        dto.setSubTotal(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));

        return dto;
    }



    @Override
    @Transactional
    public CartItemDto updateCommercialCartItem(String vetMatricule, Long itemId, CommercialCartItemRequest req, Long commercialUserId) {
        CartOrder cart = loadOrCreateCommercialCart(vetMatricule);

        OrderItem item = itemRepo.findById(itemId)
                .orElseThrow(() -> new EntityNotFoundException("Ligne panier introuvable"));

        if (!item.getOrderId().equals(cart.getId())) {
            throw new IllegalArgumentException("Cette ligne n'appartient pas au panier du vétérinaire");
        }

        if (req.getQuantity() == null || req.getQuantity() <= 0) {
            itemRepo.delete(item);
            recalculateTotal(cart.getId());
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
        recalculateTotal(cart.getId());

        Product p = productRepo.findById(item.getProductId()).orElseThrow();
        ProductVariant currentVariant = variantRepo.findById(item.getVariantId())
                .orElseThrow(() -> new EntityNotFoundException("Variant not found"));

        CartItemDto dto = new CartItemDto();
        dto.setItemId(item.getId());
        dto.setProductId(item.getProductId());
        dto.setProductName(p.getName());
        dto.setImageUrl(p.getImageUrl());
        dto.setVariantId(item.getVariantId());
        dto.setPackaging(currentVariant.getPackaging());
        dto.setQuantity(item.getQuantity());
        dto.setPrice(item.getPrice());
        dto.setSubTotal(item.getPrice().multiply(BigDecimal.valueOf(req.getQuantity())));
        return dto;
    }




//    @Override
//    @Transactional
//    public CartItemDto updateCommercialCartItem(String vetMatricule, Long itemId, Integer quantity, Long commercialUserId) {
//        CartOrder cart = loadOrCreateCommercialCart(vetMatricule);
//
//        OrderItem item = itemRepo.findById(itemId)
//                .orElseThrow(() -> new EntityNotFoundException("Ligne panier introuvable"));
//
//        if (!item.getOrderId().equals(cart.getId())) {
//            throw new IllegalArgumentException("Cette ligne n'appartient pas au panier du vétérinaire");
//        }
//
//        if (quantity == null || quantity <= 0) {
//            itemRepo.delete(item);
//            recalculateTotal(cart.getId());
//            return null;
//        }
//
//        item.setQuantity(quantity);
//        itemRepo.save(item);
//        recalculateTotal(cart.getId());
//
//        Product p = productRepo.findById(item.getProductId()).orElseThrow();
//
//        CartItemDto dto = new CartItemDto();
//        dto.setItemId(item.getId());
//        dto.setProductId(item.getProductId());
//        dto.setProductName(p.getName());
//        dto.setQuantity(item.getQuantity());
//        dto.setPrice(item.getPrice());
//        dto.setSubTotal(item.getPrice().multiply(BigDecimal.valueOf(quantity)));
//        return dto;
//    }

    private void recalculateTotal(Long orderId) {
        List<OrderItem> items = itemRepo.findByOrderId(orderId);
        BigDecimal total = items.stream()
                .map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        CartOrder order = cartOrderRepo.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Cart/order introuvable : " + orderId));

        order.setTotalAmount(total);
        cartOrderRepo.save(order);
    }

    @Override
    @Transactional
    public void removeCommercialCartItem(String vetMatricule, Long itemId, Long commercialUserId) {
        CartOrder cart = loadOrCreateCommercialCart(vetMatricule);
        OrderItem item = itemRepo.findById(itemId)
                .orElseThrow(() -> new EntityNotFoundException("Ligne introuvable"));

        if (!item.getOrderId().equals(cart.getId())) {
            throw new IllegalArgumentException("Ligne n'appartient pas à ce panier");
        }

        itemRepo.delete(item);
        recalculateTotal(cart.getId());
    }

    @Override
    @Transactional
    public void clearCommercialCart(String vetMatricule, Long commercialUserId) {
        CartOrder cart = loadOrCreateCommercialCart(vetMatricule);
        itemRepo.deleteByOrderId(cart.getId());
        cart.setTotalAmount(BigDecimal.ZERO);
        cartOrderRepo.save(cart);
    }

    @Override
    @Transactional
    public CommercialOrderConfirmationResponse confirmCommercialOrder(String vetMatricule, Long commercialUserId) {
        CartOrder cart = cartOrderRepo.findByVetMatriculeAndStatusAndOrderedByCommercial(
                        vetMatricule, OrderStatus.CART, true)
                .orElseThrow(() -> new EntityNotFoundException("Aucun panier commercial trouvé pour ce vétérinaire"));

        if (cart.getTotalAmount() == null || cart.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Impossible de confirmer un panier vide");
        }


        OurVeterinaire veterinaire = ourVeterinaireRepository.findByMatricule(vetMatricule)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Vétérinaire non trouvé pour le matricule : " + vetMatricule));

        cart.setStatus(OrderStatus.CONFIRMED);
        String orderNumber = "COM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        cart.setOrderNumber(orderNumber);
        cart.setConfirmedAt(LocalDateTime.now());
        cartOrderRepo.save(cart);

        sendCommercialOrderEmail(veterinaire, cart);

        CommercialOrderConfirmationResponse resp = new CommercialOrderConfirmationResponse();
        resp.setOrderNumber(orderNumber);
        resp.setVetMatricule(vetMatricule);
        resp.setVetEmail(veterinaire.getEmail());
        resp.setTotalAmount(cart.getTotalAmount());
        return resp;
    }

    private void sendCommercialOrderEmail(OurVeterinaire user, CartOrder order) {
        MimeMessage message = mailSender.createMimeMessage();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String orderDateStr = order.getConfirmedAt().format(formatter);

        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(user.getEmail());
            helper.setCc(financeEmail);
            helper.setSubject("Commande passée pour vous – VITALFEED");

            String nom = user.getNom() != null ? user.getNom() : "Cher vétérinaire";

            List<OrderItem> items = itemRepo.findByOrderId(order.getId());
            StringBuilder itemsHtml = new StringBuilder();

            for (OrderItem item : items) {
                Product product = productRepo.findById(item.getProductId())
                        .orElseThrow(() -> new EntityNotFoundException("Produit introuvable"));

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
                    <td style="padding:12px; text-align:right; vertical-align:middle; font-weight:600;">%.2f TND</td>
                    <td style="padding:12px; text-align:right; vertical-align:middle; font-weight:600;">%.2f TND</td>
                </tr>
                """.formatted(
                        imageUrl,
                        productName,
                        productName,
                        packaging,       // ✅
                        item.getQuantity(),
                        item.getPrice(),
                        subTotal
                ));
            }

            String totalStr = String.format("%.2f", order.getTotalAmount());

            String htmlContent = """
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <title>Commande Commerciale – VITALFEED</title>
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
                Un commercial a passé une commande en votre nom.<br>
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

            log.info("Email commande commerciale envoyé à {} (matricule {})",
                    user.getEmail(),
                    user.getMatricule());

        } catch (MessagingException e) {
            log.error("Échec envoi email commercial", e);
        }
    }

}
