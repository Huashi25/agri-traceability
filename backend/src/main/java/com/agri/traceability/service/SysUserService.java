package com.agri.traceability.service;

import com.agri.traceability.entity.SysUser;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Map;

public interface SysUserService extends IService<SysUser> {
    Map<String, Object> login(String username, String password);
    boolean register(SysUser user);

    /**
     * 查询某个登录账号绑定的农户ID。
     *
     * <p>JWT 里只放了 userId / username / role，没有放 farmer_id，
     * 所以农户相关的归属判定需要回查 sys_user。这里统一收口，
     * 避免各个 Controller 各自写一遍查询逻辑。</p>
     *
     * @param userId 登录账号ID（对应 sys_user.id）
     * @return 绑定的 farmer_info.id；未绑定或账号不存在时返回 null
     */
    Long getFarmerId(Long userId);
}
