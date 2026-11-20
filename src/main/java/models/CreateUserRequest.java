package models;

import generators.GeneratingRule;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateUserRequest extends BaseModel {
    @GeneratingRule(regex = "[A-Za-z0-9]{3,15}")
    private String username;

    // 3 заглавные, 5 строчных, 3 цифры и спецсимвол
    @GeneratingRule(regex = "[A-Z]{3}[a-z]{5}[0-9]{3}[@#$%^&+=]")
    private String password;

    // только ADMIN или USER
    @GeneratingRule(regex = "^USER$")
    private String role;
}
