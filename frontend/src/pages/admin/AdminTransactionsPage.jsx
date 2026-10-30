/**
 * AdminTransactionsPage — admin transaction monitoring.
 */

import { useEffect, useState } from "react";
import AdminLayout from "../../components/AdminLayout.jsx";
import { getAdminTransactions } from "../../api/adminService.js";
import { formatINR } from "../../utils/currency.js";
import { formatDate } from "../../utils/date.js";
import "../../styles/admin-list.css";

export default function AdminTransactionsPage() {
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [filters, setFilters] = useState({ status: "ALL" });

  useEffect(() => {
    const loadTransactions = async () => {
      setLoading(true);
      setError("");
      try {
        const data = await getAdminTransactions({
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

  return (
    <AdminLayout>
      <div className="admin-list">
        <h1>💳 Transactions</h1>

        <div className="filters-card" style={{ marginBottom: "2rem" }}>
          <select
            value={filters.status}
            onChange={(e) => {
              setFilters({ ...filters, status: e.target.value });
              setPage(0);
            }}
            style={{
              padding: "0.75rem",
              border: "1px solid #d1d5db",
              borderRadius: "4px",
              fontFamily: "inherit",
            }}
          >
            <option value="ALL">All Statuses</option>
            <option value="PENDING">Pending</option>
            <option value="SUCCESS">Success</option>
            <option value="FAILED">Failed</option>
          </select>
        </div>

        {error && <div className="alert alert-error">{error}</div>}

        {loading ? (
          <div style={{ padding: "2rem" }}>Loading transactions...</div>
        ) : transactions.length > 0 ? (
          <>
            <div className="list-card">
              <table className="admin-table">
                <thead>
                  <tr>
                    <th>Date</th>
                    <th>From</th>
                    <th>To</th>
                    <th>Amount</th>
                    <th>Status</th>
                  </tr>
                </thead>
                <tbody>
                  {transactions.map((tx) => (
                    <tr key={tx.transactionId}>
                      <td>{formatDate(tx.createdAt)}</td>
                      <td>{tx.senderEmail}</td>
                      <td>{tx.receiverEmail}</td>
                      <td>{formatINR(tx.amount)}</td>
                      <td>
                        <span className={`status-badge ${tx.status.toLowerCase()}`}>
                          {tx.status}
                        </span>
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
          <div style={{ padding: "2rem" }}>No transactions found</div>
        )}
      </div>
    </AdminLayout>
  );
}
