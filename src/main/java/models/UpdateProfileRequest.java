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
public class UpdateProfileRequest extends BaseModel {
    @GeneratingRule(regex = GeneratingRules.PROFILE_NAME)
    private String name;
}
