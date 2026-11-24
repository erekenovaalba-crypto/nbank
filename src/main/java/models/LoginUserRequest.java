package models;

import generators.GeneratingRule;
import generators.GeneratingRules;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginUserRequest extends BaseModel{
    @GeneratingRule(regex = GeneratingRules.USERNAME)
    private String username;
    @GeneratingRule(regex = GeneratingRules.PASSWORD)
    private String password;
    @GeneratingRule(regex = GeneratingRules.USER_ROLE)
    private String role;
}
