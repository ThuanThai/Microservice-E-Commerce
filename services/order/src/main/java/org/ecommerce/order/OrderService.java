package org.ecommerce.order.order;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.ecommerce.order.customer.CustomerClient;
import org.ecommerce.order.customer.CustomerResponse;
import org.ecommerce.order.exception.BusinessException;
import org.ecommerce.order.kafka.OrderProducer;
import org.ecommerce.order.orderline.OrderLineRequest;
import org.ecommerce.order.orderline.OrderLineService;
import org.ecommerce.order.payment.PaymentClient;
import org.ecommerce.order.payment.PaymentRequest;
import org.ecommerce.order.product.ProductClient;
import org.ecommerce.order.product.PurchaseRequest;
import org.ecommerce.order.product.PurchaseResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository repository;
    private final CustomerClient customerClient;
    private final ProductClient productClient;
    private final PaymentClient paymentClient;
    private final OrderLineService orderLineService;
    private final OrderMapper mapper;
    private final OrderProducer orderProducer;

    public Integer createOrder(@Valid OrderRequest request) {
        CustomerResponse customer = this.customerClient.findCustomerById(request.customerId())
                .orElseThrow(() -> new BusinessException("Customer not found"));

        List<PurchaseResponse> purchasedProducts = productClient.purchaseProducts(request.products());

        Order order = this.repository.save(mapper.toOrder(request));

        for (PurchaseRequest purchaseRequest : request.products()) {
            orderLineService.saveOrderLine(
                    new OrderLineRequest(
                            null,
                            order.getId(),
                            purchaseRequest.productId(),
                            purchaseRequest.quantity()
                    )
            );
        }
        PaymentRequest paymentRequest = new PaymentRequest(
                request.amount(),
                request.paymentMethod(),
                order.getId(),
                order.getReference(),
                customer
        );
        paymentClient.requestOrderPayment(paymentRequest);

        orderProducer.sendOrderConfirmation(
                new OrderConfirmation(
                        request.reference(),
                        request.amount(),
                        request.paymentMethod(),
                        customer,
                        purchasedProducts
                )
        );

        return order.getId();
    }

    public List<OrderResponse> findAllOrders() {
        return this.repository.findAll()
                .stream()
                .map(this.mapper::fromOrder)
                .collect(Collectors.toList());
    }

    public OrderResponse findById(Integer id) {
        return this.repository.findById(id)
                .map(this.mapper::fromOrder)
                .orElseThrow(() -> new EntityNotFoundException(String.format("No order found with the provided ID: %d", id)));
    }
}
