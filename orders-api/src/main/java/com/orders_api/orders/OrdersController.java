package com.orders_api.orders;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/orders")
public class OrdersController {

  private final List<Map<String, Object>> store = new ArrayList<>();

  @GetMapping
  public List<Map<String, Object>> list() {
    return store; // requires SCOPE_read:orders (see SecurityConfig)
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Map<String, Object> create(@RequestBody Map<String, Object> body) {
    body.put("id", UUID.randomUUID().toString());
    store.add(body);
    return body; // requires SCOPE_write:orders
  }
}
