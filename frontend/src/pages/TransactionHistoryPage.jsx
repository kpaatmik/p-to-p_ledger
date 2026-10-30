/**
 * TransactionHistoryPage — paginated transaction list with filters.
 */

import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import Layout from "../components/Layout.jsx";
import { getTransactions } from "../api/transferService.js";
import ROUTES from "../constants/routes.js";
import { formatINR } from "../utils/currency.js";
import { formatDate } from "../utils/date.js";
import "../styles/list-page.css";

export default function TransactionHistoryPage() {
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [filters, setFilters] = useState({
    type: "ALL",
    status: "ALL",
  });

  useEffect(() => {
    const loadTransactions = async () => {
      setLoading(true);
      setError("");
      try {
        const data = await getTransactions({
          type: filters.type === "ALL" ? null : filters.type,
          status: filters.status === "ALL" ? null : filters.status,
          page,
          size: 10,
        });
        setTransactions(data.content || []);
        setTotalPages(data.totalPages || 0);
      } catch (err) {
        setError("Failed to load transactions");
        console.error(err);
      } finally {
        setLoading(false);
      }
    };

    loadTransactions();
  }, [page, filters]);

  const handleFilterChange = (e) => {
    const { name, value } = e.target;
    setFilters((prev) => ({ ...prev, [name]: value }));
    setPage(0);
  };

  return (
    <Layout>
      <div className="list-page">
        <h1>📊 Transaction History</h1>

        <div className="filters-card">
          <div className="filter-group">
            <label htmlFor="type">Type</label>
            <select
              id="type"
              name="type"
              value={filters.type}
              onChange={handleFilterChange}
            >
              <option value="ALL">All</option>
              <option value="SENT">Sent</option>
              <option value="RECEIVED">Received</option>
            </select>
          </div>

          <div className="filter-group">
            <label htmlFor="status">Status</label>
            <select
              id="status"
              name="status"
              value={filters.status}
              onChange={handleFilterChange}
            >
              <option value="ALL">All</option>
              <option value="SUCCESS">Success</option>
              <option value="PENDING">Pending</option>
              <option value="FAILED">Failed</option>
            </select>
          </div>
        </div>

        {error && <div className="alert alert-error">{error}</div>}

        {loading ? (
          <div className="loading-state">Loading transactions...</div>
        ) : transactions.length > 0 ? (
          <>
            <div className="list-card">
              <table className="transaction-table">
                <thead>
                  <tr>
                    <th>Date</th>
                    <th>Party</th>
                    <th>Amount</th>
                    <th>Status</th>
                    <th>Action</th>
                  </tr>
                </thead>
                <tbody>
                  {transactions.map((tx) => (
                    <tr key={tx.transactionId}>
                      <td>{formatDate(tx.createdAt)}</td>
                      <td>{tx.otherPartyEmail}</td>
                      <td className={`amount ${tx.type.toLowerCase()}`}>
                        {tx.type === "SENT" ? "-" : "+"} {formatINR(tx.amount)}
                      </td>
                      <td>
                        <span className={`status-badge ${tx.status.toLowerCase()}`}>
                          {tx.status}
                        </span>
                      </td>
                      <td>
                        <Link
                          to={ROUTES.TRANSACTION_DETAIL.replace(
                            ":id",
                            tx.transactionId
                          )}
                          className="link"
                        >
                          View
                        </Link>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            <div className="pagination">
              <button
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={page === 0}
                className="btn btn-small"
              >
                ← Previous
              </button>
              <span>
                Page {page + 1} of {totalPages}
              </span>
              <button
                onClick={() => setPage((p) => p + 1)}
                disabled={page >= totalPages - 1}
                className="btn btn-small"
              >
                Next →
              </button>
            </div>
          </>
        ) : (
          <div className="empty-state">No transactions found</div>
        )}
      </div>
    </Layout>
  );
}
