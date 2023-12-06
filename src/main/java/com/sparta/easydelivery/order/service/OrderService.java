package com.sparta.easydelivery.order.service;

import com.sparta.easydelivery.cart.entity.Cart;
import com.sparta.easydelivery.cart.service.CartService;
import com.sparta.easydelivery.order.dto.OrderMapResponseDto;
import com.sparta.easydelivery.order.dto.OrderRequestDto;
import com.sparta.easydelivery.order.dto.OrderResponseDto;
import com.sparta.easydelivery.order.dto.OrderStatusRequestDto;
import com.sparta.easydelivery.order.entity.Order;
import com.sparta.easydelivery.order.entity.OrderStatusEnum;
import com.sparta.easydelivery.order.exception.NotFoundOrderException;
import com.sparta.easydelivery.order.exception.NotMatchUserException;
import com.sparta.easydelivery.order.exception.OrderNothingException;
import com.sparta.easydelivery.order.exception.UnauthorizedAccessException;
import com.sparta.easydelivery.order.repository.OrderRepository;
import com.sparta.easydelivery.user.entity.User;
import com.sparta.easydelivery.user.entity.UserRoleEnum;
import com.sparta.easydelivery.user.service.UserService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;

    private final OrderProductService orderProductService;

    private final CartService cartService;

    private final UserService userService;

    public OrderResponseDto createOrder(OrderRequestDto requestDto, User user){
        // dto -> entity
        Order order = new Order(requestDto, user);
        List<Cart> cartList = cartService.getCartList(user); //사용자의 장바구니
        if(cartList.isEmpty()){
            throw new OrderNothingException("주문 할 상품이 없습니다.");
        }
        orderProductService.ConvertCartToOrderProduct(cartList, order); //장바구니에 있는 상품을 ProductOrder에 옮겨 저장
        order.setTotalPrice(cartList); // 장바구니 상품 가격*수량의 합계를 총가격에 저장
        cartService.clearCart(user); // 장바구니 비우기

        Order saveOrder = orderRepository.save(order);

        return new OrderResponseDto(saveOrder);
    }

    public OrderMapResponseDto getOrders(User user){
        OrderMapResponseDto orderMap = new OrderMapResponseDto();

        if(user.getRole().equals(UserRoleEnum.USER)) {  // 사용자가 조회한 경우
            for(OrderStatusEnum status : OrderStatusEnum.values()){
                orderMap.setOrderToMap(status, orderRepository.findAllByUserAndStatus(user, status).stream()
                    .map(OrderResponseDto::new).toList());
            }
        }
        else{ // 관리자가 조회한 경우
            for(OrderStatusEnum status : OrderStatusEnum.values()){
                orderMap.setOrderToMap(status, orderRepository.findAllByStatus(status).stream()
                    .map(OrderResponseDto::new).toList());
            }
        }

        return orderMap;
    }

    public OrderResponseDto getOrder(Long orderId, User user){
        Order order = getOrderEntity(orderId, user);
        return new OrderResponseDto(order);
    }

    @Transactional
    public OrderResponseDto updateOrderStatus(Long orderId, OrderStatusRequestDto requestDto, User user){
        userService.isAdminOrException(user);
        Order order = getOrderEntity(orderId, user);
        order.setStatus(requestDto.getOrderStatusEnum());
        return new OrderResponseDto(order);
    }

    public void deleteOrder(Long orderId, User user){
        Order order = getOrderEntity(orderId, user);
        orderRepository.delete(order);
    }

    public Order getOrderEntity(Long orderId, User user) {
        Order order = orderRepository.findById(orderId).orElseThrow(
            () -> new NotFoundOrderException("해당 주문이 존재하지 않습니다.")
        );
        if(user.getRole() == UserRoleEnum.USER && order.getUser().getUsername().equals(user.getUsername())){
            throw new NotMatchUserException("해당 주문은 사용자의 주문이 아닙니다.");
        }
        return order;
    }

}
