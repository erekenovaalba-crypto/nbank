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
public class TransferRequest extends BaseModel {
    @GeneratingRule(regex = GeneratingRules.ACCOUNT_ID)
    private long senderAccountId;
    @GeneratingRule(regex = GeneratingRules.ACCOUNT_ID)
    private long receiverAccountId;
    @GeneratingRule(regex = GeneratingRules.TRANSFER_AMOUNT)
    private BigDecimal amount;
}