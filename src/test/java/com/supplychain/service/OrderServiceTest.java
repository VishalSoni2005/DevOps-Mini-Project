package com.supplychain.service;

import com.supplychain.exception.ResourceNotFoundException;
import com.supplychain.exception.SupplyChainException;
import com.supplychain.model.Order;
import com.supplychain.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderService orderService;

    private Order sampleOrder;

    @BeforeEach
    void setUp() {
        sampleOrder = new Order();
        sampleOrder.setId(1L);
        sampleOrder.setOrderNumber("PO-TEST-001");
        sampleOrder.setOrderType(Order.OrderType.PURCHASE);
        sampleOrder.setStatus(Order.OrderStatus.PENDING);
        sampleOrder.setSupplier("TestSupplier");
        sampleOrder.setDestination("Warehouse-A");
        sampleOrder.setTotalAmount(5000.0);
    }

    @Test
    @DisplayName("Should return all orders")
    void getAllOrders_ReturnsOrderList() {
        when(orderRepository.findAll()).thenReturn(Arrays.asList(sampleOrder));

        List<Order> orders = orderService.getAllOrders();

        assertNotNull(orders);
        assertEquals(1, orders.size());
    }

    @Test
    @DisplayName("Should return order by ID")
    void getOrderById_ExistingId_ReturnsOrder() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(sampleOrder));

        Order found = orderService.getOrderById(1L);

        assertEquals("PO-TEST-001", found.getOrderNumber());
    }

    @Test
    @DisplayName("Should throw exception for non-existent order")
    void getOrderById_NonExistentId_ThrowsException() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> orderService.getOrderById(999L));
    }

    @Test
    @DisplayName("Should create a new order")
    void createOrder_ValidOrder_ReturnsCreated() {
        when(orderRepository.findByOrderNumber("PO-TEST-001")).thenReturn(Optional.empty());
        when(orderRepository.save(any(Order.class))).thenReturn(sampleOrder);

        Order created = orderService.createOrder(sampleOrder);

        assertNotNull(created);
        assertEquals("PO-TEST-001", created.getOrderNumber());
    }

    @Test
    @DisplayName("Should throw exception for duplicate order number")
    void createOrder_DuplicateNumber_ThrowsException() {
        when(orderRepository.findByOrderNumber("PO-TEST-001")).thenReturn(Optional.of(sampleOrder));

        assertThrows(SupplyChainException.class, () -> orderService.createOrder(sampleOrder));
    }

    @Test
    @DisplayName("Should update order status successfully")
    void updateOrderStatus_ValidTransition_UpdatesStatus() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(sampleOrder));
        sampleOrder.setStatus(Order.OrderStatus.CONFIRMED);
        when(orderRepository.save(any(Order.class))).thenReturn(sampleOrder);

        Order updated = orderService.updateOrderStatus(1L, Order.OrderStatus.CONFIRMED);

        assertEquals(Order.OrderStatus.CONFIRMED, updated.getStatus());
    }

    @Test
    @DisplayName("Should reject invalid status transition from DELIVERED")
    void updateOrderStatus_FromDelivered_ThrowsException() {
        sampleOrder.setStatus(Order.OrderStatus.DELIVERED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(sampleOrder));

        assertThrows(SupplyChainException.class,
                () -> orderService.updateOrderStatus(1L, Order.OrderStatus.PROCESSING));
    }

    @Test
    @DisplayName("Should reject invalid status transition from CANCELLED")
    void updateOrderStatus_FromCancelled_ThrowsException() {
        sampleOrder.setStatus(Order.OrderStatus.CANCELLED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(sampleOrder));

        assertThrows(SupplyChainException.class,
                () -> orderService.updateOrderStatus(1L, Order.OrderStatus.PENDING));
    }

    @Test
    @DisplayName("Should return orders by status")
    void getOrdersByStatus_ReturnsFilteredList() {
        when(orderRepository.findByStatus(Order.OrderStatus.PENDING))
                .thenReturn(Arrays.asList(sampleOrder));

        List<Order> orders = orderService.getOrdersByStatus(Order.OrderStatus.PENDING);

        assertEquals(1, orders.size());
        assertEquals(Order.OrderStatus.PENDING, orders.get(0).getStatus());
    }
}
