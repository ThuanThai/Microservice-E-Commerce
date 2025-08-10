package org.ecommerce.order.customer;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Optional;

@FeignClient(
        name = "customer-service",
        url = "${application.config.customer-url}"
)
@Service
public interface CustomerClient {
    @GetMapping("/{customer-id}")
    Optional<CustomerResponse> findCustomerById(@PathVariable("customer-id") String customerId);
}
