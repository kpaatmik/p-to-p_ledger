/**
 * TransactionDetailPage — detailed view of a single transaction with ledger entries.
 */

import { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import Layout from "../components/Layout.jsx";
import { getTransactionDetail } from "../api/transferService.js";
import ROUTES from "../constants/routes.js";
import { formatINR } from "../utils/currency.js";
import { formatDate, formatDateTime } from "../utils/date.js";
import "../styles/detail-page.css";

export default function TransactionDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [transaction, setTransaction] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    const loadTransaction = async () => {
      setLoading(true);
      setError("");
      try {
        const data = await getTransactionDetail(id);
        setTransaction(data);
      } catch (err) {
        setError("Failed to load transaction details");
        console.error(err);
      } finally {
        setLoading(false);
      }
    };

    loadTransaction();
  }, [id]);

  if (loading) {
    return (
      <Layout>
        <div className="loading-state">Loading transaction details...</div>
      </Layout>
    );
  }

  return (
    <Layout>
      <div className="detail-page">
        <button 
          onClick={() => navigate(ROUTES.TRANSACTIONS)} 
          className="btn btn-small"
          style={{ marginBottom: "1.5rem" }}
        >
          ← Back to Transactions
        </button>

        {error && <div className="alert alert-error">{error}</div>}

        {transaction && (
          <div className="detail-card">
            <div className="detail-header">
              <h1>Transaction Details</h1>
              <span className={`status-badge ${transaction.status.toLowerCase()}`}>
                {transaction.status}
              </span>
            </div>

            <div className="detail-section">
              <h2>Overview</h2>
              <div className="detail-grid">
                <div className="detail-item">
                  <label>Transaction ID</label>
                  <p className="mono">{transaction.transactionId}</p>
                </div>
                <div className="detail-item">
                  <label>Amount</label>
                  <p>{formatINR(transaction.amount)}</p>
                </div>
                <div className="detail-item">
                  <label>Status</label>
                  <p>{transaction.status}</p>
                </div>
                <div className="detail-item">
                  <label>Type</label>
                  <p>{transaction.type || "P2P Transfer"}</p>
                </div>
              </div>
            </div>

            <div className="detail-section">
              <h2>Parties</h2>
              <div className="detail-grid">
                <div className="detail-item">
                  <label>From</label>
                  <p>{transaction.senderEmail}</p>
                </div>
                <div className="detail-item">
                  <label>To</label>
                  <p>{transaction.recipientEmail}</p>
                </div>
              </div>
            </div>

            <div className="detail-section">
              <h2>Timeline</h2>
              <div className="detail-grid">
                <div className="detail-item">
                  <label>Initiated</label>
                  <p>{formatDateTime(transaction.createdAt)}</p>
                </div>
                <div className="detail-item">
                  <label>Settlement Due</label>
                  <p>{transaction.settlementDueDate || "N/A"}</p>
                </div>
                {transaction.settledAt && (
                  <div className="detail-item">
                    <label>Settled</label>
                    <p>{formatDateTime(transaction.settledAt)}</p>
                  </div>
                )}
              </div>
            </div>

            {transaction.status === "PENDING" && (
              <div className="info-box">
                <strong>⏳ Pending Settlement</strong>
                <p>
                  This transfer is pending and will be settled in approximately {transaction.daysUntilSettlement} day(s).
                  The sender's funds have been debited. After settlement, the recipient will receive the funds.
                </p>
              </div>
            )}
          </div>
        )}
      </div>
    </Layout>
  );
}
