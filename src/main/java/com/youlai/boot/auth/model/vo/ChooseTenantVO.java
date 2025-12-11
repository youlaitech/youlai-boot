package com.youlai.boot.auth.model.vo;

import com.youlai.boot.system.model.vo.TenantVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 选择租户响应VO
 *
 * @author Ray.Hao
 * @since 3.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "选择租户响应")
public class ChooseTenantVO implements Serializable {

    @Schema(description = "租户列表")
    private List<TenantVO> tenants;
}

