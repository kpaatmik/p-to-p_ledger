/**
 * MiniStatement — displays the latest 10 transactions.
 */

import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { getMiniStatement } from "../api/transferService.js";
import ROUTES from "../constants/routes.js";
import { formatINR } from "../utils/currency.js";
import { formatDate } from "../utils/date.js";
import "../styles/mini-statement.css";

export default function MiniStatement() {
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    const loadTransactions = async () => {
      setLoading(true);
      setError("");
      try {
        const data = await getMiniStatement();
        setTransactions(data.content || []);
      } catch (err) {
        setError("Failed to load transaction history");
        console.error(err);
      } finally {
        setLoading(false);
      }
    };

    loadTransactions();
  }, []);

  if (loading) return <div className="mini-statement loading">Loading...</div>;
  if (error) return <div className="mini-statement error">{error}</div>;

  return (
    <div className="mini-statement">
      <div className="mini-statement-header">
        <h3>Recent Transactions</h3>
        <Link to={ROUTES.TRANSACTIONS}>View All →</Link>
      </div>
      {transactions.length > 0 ? (
        <table className="transaction-table">
          <tbody>
            {transactions.map((tx) => (
              <tr key={tx.transactionId} className="transaction-row">
                <td>
                  <Link
                    to={ROUTES.TRANSACTION_DETAIL.replace(
                      ":id",
                      tx.transactionId
                    )}
                  >
                    <strong>{tx.otherPartyEmail}</strong>
                    <small>{formatDate(tx.createdAt)}</small>
                  </Link>
                </td>
                <td className={`amount ${tx.type.toLowerCase()}`}>
                  {tx.type === "SENT" ? "-" : "+"} {formatINR(tx.amount)}
                </td>
                <td>
                  <span className={`status-badge ${tx.status.toLowerCase()}`}>
                    {tx.status}
                  </span>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      ) : (
        <p className="empty-state">No transactions yet</p>
      )}
    </div>
  );
}
