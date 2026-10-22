package com.supplychain.service;

import com.supplychain.exception.ResourceNotFoundException;
import com.supplychain.exception.SupplyChainException;
import com.supplychain.model.Order;
import com.supplychain.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service handling all order-related operations.
 * Logs every state transition for full traceability in ELK.
 */
@Service
@Transactional
public class OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public List<Order> getAllOrders() {
        logger.info("ORDER_EVENT: action=LIST_ALL_ORDERS");
        return orderRepository.findAll();
    }

    public Order getOrderById(Long id) {
        logger.info("ORDER_EVENT: action=GET_ORDER, orderId={}", id);
        return orderRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("ORDER_ERROR: action=GET_ORDER, orderId={}, error=NOT_FOUND", id);
                    return new ResourceNotFoundException("Order", "id", id);
                });
    }

    public Order getOrderByNumber(String orderNumber) {
        logger.info("ORDER_EVENT: action=GET_ORDER_BY_NUMBER, orderNumber={}", orderNumber);
        return orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> {
                    logger.error("ORDER_ERROR: action=GET_ORDER_BY_NUMBER, orderNumber={}, error=NOT_FOUND", orderNumber);
                    return new ResourceNotFoundException("Order", "orderNumber", orderNumber);
                });
    }

    public Order createOrder(Order order) {
        logger.info("ORDER_EVENT: action=CREATE_ORDER, orderNumber={}, type={}, supplier={}",
                order.getOrderNumber(), order.getOrderType(), order.getSupplier());

        // Check for duplicate order number
        orderRepository.findByOrderNumber(order.getOrderNumber()).ifPresent(existing -> {
            logger.error("ORDER_ERROR: action=CREATE_ORDER, orderNumber={}, error=DUPLICATE_ORDER",
                    order.getOrderNumber());
            throw new SupplyChainException(
                    "Order '" + order.getOrderNumber() + "' already exists", "DUPLICATE_ORDER");
        });

        Order saved = orderRepository.save(order);
        logger.info("ORDER_EVENT: action=ORDER_CREATED, orderId={}, orderNumber={}, status={}",
                saved.getId(), saved.getOrderNumber(), saved.getStatus());
        return saved;
    }

    public Order updateOrderStatus(Long id, Order.OrderStatus newStatus) {
        logger.info("ORDER_EVENT: action=UPDATE_STATUS, orderId={}, newStatus={}", id, newStatus);

        Order order = getOrderById(id);
        Order.OrderStatus oldStatus = order.getStatus();

        // Validate state transitions
        validateStatusTransition(order, oldStatus, newStatus);

        order.setStatus(newStatus);
        Order updated = orderRepository.save(order);

        logger.info("ORDER_EVENT: action=STATUS_UPDATED, orderId={}, orderNumber={}, oldStatus={}, newStatus={}",
                updated.getId(), updated.getOrderNumber(), oldStatus, newStatus);

        if (newStatus == Order.OrderStatus.FAILED) {
            logger.error("ORDER_CRITICAL: action=ORDER_FAILED, orderId={}, orderNumber={}, supplier={}",
                    order.getId(), order.getOrderNumber(), order.getSupplier());
        }

        if (newStatus == Order.OrderStatus.DELIVERED) {
            logger.info("ORDER_EVENT: action=ORDER_DELIVERED, orderId={}, orderNumber={}", order.getId(), order.getOrderNumber());
        }

        return updated;
    }

    public void deleteOrder(Long id) {
        logger.info("ORDER_EVENT: action=DELETE_ORDER, orderId={}", id);
        Order order = getOrderById(id);
        orderRepository.delete(order);
        logger.warn("ORDER_EVENT: action=ORDER_DELETED, orderId={}, orderNumber={}", id, order.getOrderNumber());
    }

    public List<Order> getOrdersByStatus(Order.OrderStatus status) {
        logger.info("ORDER_EVENT: action=QUERY_BY_STATUS, status={}", status);
        return orderRepository.findByStatus(status);
    }

    /**
     * Validates that the status transition is allowed.
     */
    private void validateStatusTransition(Order order, Order.OrderStatus from, Order.OrderStatus to) {
        // CANCELLED and DELIVERED are terminal states
        if (from == Order.OrderStatus.CANCELLED || from == Order.OrderStatus.DELIVERED) {
            logger.error("ORDER_ERROR: action=INVALID_TRANSITION, orderId={}, from={}, to={}",
                    order.getId(), from, to);
            throw new SupplyChainException(
                    "Cannot transition from " + from + " to " + to, "INVALID_STATUS_TRANSITION");
        }
    }
}
