package models;

import generators.GeneratingRule;
import generators.GeneratingRules;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepositRequest extends BaseModel {
    // id счёта; в тестах подменяется реальным
    @GeneratingRule(regex = GeneratingRules.ACCOUNT_ID)
    private long id;
    @GeneratingRule(regex = GeneratingRules.DEPOSIT_AMOUNT)
    private BigDecimal balance;
}