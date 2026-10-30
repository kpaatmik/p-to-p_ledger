/**
 * AdminFraudReviewPage — fraud alert queue with block/dismiss actions.
 * Endpoint: GET /admin/fraud-alerts  (AlertStatus filter: PENDING | BLOCKED | DISMISSED)
 * Block:    PATCH /admin/fraud-alerts/{alertId}/block
 * Dismiss:  PATCH /admin/fraud-alerts/{alertId}/dismiss
 */

import { useEffect, useState } from "react";
import AdminLayout from "../../components/AdminLayout.jsx";
import {
  getFraudAlerts,
  blockFraudAlert,
  dismissFraudAlert,
} from "../../api/adminService.js";
import { formatINR } from "../../utils/currency.js";
import { formatDateTime } from "../../utils/date.js";
import "../../styles/admin-list.css";

export default function AdminFraudReviewPage() {
  const [alerts, setAlerts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [statusFilter, setStatusFilter] = useState("PENDING");
  const [actioningId, setActioningId] = useState(null);

  const loadAlerts = async (currentPage = 0, status = "PENDING") => {
    setLoading(true);
    setError("");
    try {
      const data = await getFraudAlerts(status || null, currentPage, 10);
      setAlerts(data.content || []);
      setTotalPages(data.totalPages || 0);
    } catch (err) {
      setError("Failed to load fraud alerts");
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadAlerts(page, statusFilter);
  }, [page, statusFilter]);

  const handleBlock = async (alertId) => {
    if (!window.confirm("Block this transaction and refund the sender?"))
      return;
    setActioningId(alertId);
    try {
      await blockFraudAlert(alertId);
      await loadAlerts(page, statusFilter);
    } catch (err) {
      alert(err?.response?.data?.message || "Block failed");
    } finally {
      setActioningId(null);
    }
  };

  const handleDismiss = async (alertId) => {
    if (!window.confirm("Dismiss this alert? The transaction will proceed."))
      return;
    setActioningId(alertId);
    try {
      await dismissFraudAlert(alertId);
      await loadAlerts(page, statusFilter);
    } catch (err) {
      alert(err?.response?.data?.message || "Dismiss failed");
    } finally {
      setActioningId(null);
    }
  };

  return (
    <AdminLayout>
      <div className="admin-list">
        <h1>🚨 Fraud Review</h1>

        <div style={{ marginBottom: "1.5rem" }}>
          <select
            value={statusFilter}
            onChange={(e) => {
              setStatusFilter(e.target.value);
              setPage(0);
            }}
            style={{
              padding: "0.75rem",
              border: "1px solid #d1d5db",
              borderRadius: "4px",
              fontFamily: "inherit",
            }}
          >
            <option value="PENDING">Pending</option>
            <option value="BLOCKED">Blocked</option>
            <option value="DISMISSED">Dismissed</option>
            <option value="">All</option>
          </select>
        </div>

        {error && <div className="alert alert-error">{error}</div>}

        {loading ? (
          <div style={{ padding: "2rem" }}>Loading alerts...</div>
        ) : alerts.length > 0 ? (
          <>
            <div className="list-card">
              <table className="admin-table">
                <thead>
                  <tr>
                    <th>Type</th>
                    <th>From</th>
                    <th>To</th>
                    <th>Amount</th>
                    <th>Status</th>
                    <th>Created</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {alerts.map((alert) => (
                    <tr key={alert.alertId}>
                      <td>
                        <span className="status-badge pending">
                          {alert.alertType}
                        </span>
                      </td>
                      <td>{alert.senderEmail}</td>
                      <td>{alert.recipientEmail}</td>
                      <td>{formatINR(alert.amount)}</td>
                      <td>
                        <span
                          className={`status-badge ${alert.status.toLowerCase()}`}
                        >
                          {alert.status}
                        </span>
                      </td>
                      <td>{formatDateTime(alert.createdAt)}</td>
                      <td>
                        {alert.status === "PENDING" && (
                          <div style={{ display: "flex", gap: "0.5rem" }}>
                            <button
                              className="btn btn-small"
                              style={{ color: "#ef4444" }}
                              onClick={() => handleBlock(alert.alertId)}
                              disabled={actioningId === alert.alertId}
                            >
                              Block
                            </button>
                            <button
                              className="btn btn-small"
                              onClick={() => handleDismiss(alert.alertId)}
                              disabled={actioningId === alert.alertId}
                            >
                              Dismiss
                            </button>
                          </div>
                        )}
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
              <span>Page {page + 1} of {totalPages || 1}</span>
              <button
                onClick={() => setPage((p) => p + 1)}
                disabled={page >= (totalPages || 1) - 1}
                className="btn btn-small"
              >
                Next →
              </button>
            </div>
          </>
        ) : (
          <div style={{ padding: "2rem" }}>
            No {statusFilter.toLowerCase()} fraud alerts
          </div>
        )}
      </div>
    </AdminLayout>
  );
}
