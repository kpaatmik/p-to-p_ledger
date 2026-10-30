/**
 * WalletCard — displays wallet balance information and status.
 */

import { formatINR } from "../utils/currency.js";
import "../styles/wallet-card.css";

export default function WalletCard({
  totalBalance,
  availableBalance,
  reservedBalance,
  status,
  todaySpending,
}) {
  const statusColor =
    status === "ACTIVE" ? "active" : status === "FROZEN" ? "frozen" : "inactive";

  return (
    <div className={`wallet-card ${statusColor}`}>
      <div className="wallet-header">
        <h2>💰 Wallet</h2>
        <span className={`status-badge ${statusColor}`}>{status}</span>
      </div>
      <div className="wallet-balances">
        <div className="balance-item">
          <label>Total Balance</label>
          <p className="balance-amount">{formatINR(totalBalance)}</p>
        </div>
        <div className="balance-item">
          <label>Available</label>
          <p className="balance-amount available">
            {formatINR(availableBalance)}
          </p>
        </div>
        <div className="balance-item">
          <label>Reserved</label>
          <p className="balance-amount">{formatINR(reservedBalance)}</p>
        </div>
      </div>
      <div className="wallet-footer">
        <small>Today's Spending: {formatINR(todaySpending)}</small>
      </div>
    </div>
  );
}
