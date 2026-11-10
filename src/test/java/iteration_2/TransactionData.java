package iteration_2;

import java.math.BigDecimal;

public class TransactionData {
    private Integer id;
    private BigDecimal amount;
    private String type;
    private String timestamp;
    private Integer relatedAccountId;

    public TransactionData() {
    }

    public TransactionData(Integer id, BigDecimal amount, String type, String timestamp, Integer relatedAccountId) {
        this.id = id;
        this.amount = amount;
        this.type = type;
        this.timestamp = timestamp;
        this.relatedAccountId = relatedAccountId;
    }

    public Integer getId() {
        return id;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getType() {
        return type;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public Integer getRelatedAccountId() {
        return relatedAccountId;
    }
    @Override
    public String toString() {
        return "TransactionData{id=" + id + ", amount=" + amount + ", type='" + type
                + "', timestamp='" + timestamp + "', relatedAccountId=" + relatedAccountId + "}";
    }
}
