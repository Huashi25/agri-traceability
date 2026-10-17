package com.agri.traceability.controller;

import com.agri.traceability.common.Result;
import com.agri.traceability.entity.UserAddress;
import com.agri.traceability.service.UserAddressService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/api/address")
public class UserAddressController {

    @Autowired
    private UserAddressService addressService;

    @GetMapping("/list")
    public Result list(HttpServletRequest request) {
        Long userId = Long.valueOf(request.getAttribute("userId").toString());
        QueryWrapper<UserAddress> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
                .orderByDesc("is_default")
                .orderByDesc("create_time");
        return Result.success(addressService.list(wrapper));
    }

    @PostMapping
    public Result<Void> save(@RequestBody UserAddress address, HttpServletRequest request) {
        Long userId = Long.valueOf(request.getAttribute("userId").toString());
        address.setUserId(userId);

        if (address.getIsDefault() != null && address.getIsDefault() == 1) {
            QueryWrapper<UserAddress> wrapper = new QueryWrapper<>();
            wrapper.eq("user_id", userId).eq("is_default", 1);
            List<UserAddress> defaults = addressService.list(wrapper);
            for (UserAddress a : defaults) {
                a.setIsDefault(0);
                addressService.updateById(a);
            }
        }

        if (address.getId() != null) {
            addressService.updateById(address);
        } else {
            addressService.save(address);
        }
        return Result.success();
    }

    @PutMapping
    public Result<Void> update(@RequestBody UserAddress address) {
        addressService.updateById(address);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        addressService.removeById(id);
        return Result.success();
    }

    @PutMapping("/default/{id}")
    public Result<Void> setDefault(@PathVariable Long id, HttpServletRequest request) {
        Long userId = Long.valueOf(request.getAttribute("userId").toString());
        QueryWrapper<UserAddress> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).eq("is_default", 1);
        List<UserAddress> defaults = addressService.list(wrapper);
        for (UserAddress a : defaults) {
            a.setIsDefault(0);
            addressService.updateById(a);
        }
        UserAddress address = addressService.getById(id);
        if (address != null) {
            address.setIsDefault(1);
            addressService.updateById(address);
        }
        return Result.success();
    }
}
