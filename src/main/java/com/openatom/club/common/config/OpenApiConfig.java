package com.openatom.club.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.media.StringSchema;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("开放原子开源社团秘书处管理系统 API")
                        .version("1.0.0")
                        .description("""
                                # 开放原子开源社团秘书处资料与积分管理系统
                                
                                ## 权限说明
                                通过请求头传递当前操作者身份（暂不使用真实登录）：
                                - `X-Actor-Name`: 操作者姓名
                                - `X-Actor-Department`: 操作者部门
                                - `X-Actor-Position`: 操作者职务（社员/部长/会长/副会长）
                                
                                ## 权限规则
                                - **会长/副会长**：拥有全部权限
                                - **秘书处成员**：可以管理成员、积分、资料、会议纪要、财务台账
                                - **普通社员**：只能查看公开数据，可以提交活动登记
                                """)
                );
    }

    @Bean
    public OperationCustomizer actorHeaderCustomizer() {
        return (operation, handlerMethod) -> {
            operation.addParametersItem(new Parameter()
                    .in("header")
                    .name("X-Actor-Name")
                    .description("操作者姓名")
                    .schema(new StringSchema())
                    .required(false));
            operation.addParametersItem(new Parameter()
                    .in("header")
                    .name("X-Actor-Department")
                    .description("操作者部门（填'秘书处'可获得管理权限）")
                    .schema(new StringSchema())
                    .required(false));
            operation.addParametersItem(new Parameter()
                    .in("header")
                    .name("X-Actor-Position")
                    .description("操作者职务：社员/部长/会长/副会长")
                    .schema(new StringSchema())
                    .required(false));
            return operation;
        };
    }
}
