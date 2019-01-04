package rtp.demo.debtor.domain.model.transaction;

import java.io.Serializable;
import java.math.BigDecimal;

public class Transaction implements Serializable {

	private static final long serialVersionUID = 847186233458570682L;

	private String transId;
	private BigDecimal amount;
	private String status;
	private Boolean creditOrDebit;

	public String getTransId() {
		return transId;
	}

	public void setTransId(String transId) {
		this.transId = transId;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Boolean getCreditOrDebit() {
		return creditOrDebit;
	}

	public void setCreditOrDebit(Boolean creditOrDebit) {
		this.creditOrDebit = creditOrDebit;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((amount == null) ? 0 : amount.hashCode());
		result = prime * result + ((creditOrDebit == null) ? 0 : creditOrDebit.hashCode());
		result = prime * result + ((status == null) ? 0 : status.hashCode());
		result = prime * result + ((transId == null) ? 0 : transId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Transaction other = (Transaction) obj;
		if (amount == null) {
			if (other.amount != null)
				return false;
		} else if (!amount.equals(other.amount))
			return false;
		if (creditOrDebit == null) {
			if (other.creditOrDebit != null)
				return false;
		} else if (!creditOrDebit.equals(other.creditOrDebit))
			return false;
		if (status == null) {
			if (other.status != null)
				return false;
		} else if (!status.equals(other.status))
			return false;
		if (transId == null) {
			if (other.transId != null)
				return false;
		} else if (!transId.equals(other.transId))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Transaction [transId=" + transId + ", amount=" + amount + ", status=" + status + ", creditOrDebit="
				+ creditOrDebit + "]";
	}

}
