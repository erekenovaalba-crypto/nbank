package models;

import generators.GeneratingRule;
import generators.GeneratingRules;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@Builder
@Data
@NoArgsConstructor
public class CreateUserRequest extends BaseModel {
    @GeneratingRule(regex = GeneratingRules.USERNAME)
    private String username;

    @GeneratingRule(regex = GeneratingRules.PASSWORD)
    private String password;

    @GeneratingRule(regex = GeneratingRules.USER_ROLE)
    private String role;
}