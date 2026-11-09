package models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateAccountResponse extends BaseModel{
    private long id;
    private String accountNumber;
    private BigDecimal balance;
    private List<String> transaction;
}
