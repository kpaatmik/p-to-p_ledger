/**
 * AdminSettlementPage — settlement monitor with manual triggers.
 */

import { useEffect, useState } from "react";
import AdminLayout from "../../components/AdminLayout.jsx";
import { getSettlementMonitor, triggerSettlement } from "../../api/adminService.js";
import { formatINR } from "../../utils/currency.js";
import "../../styles/admin-list.css";

export default function AdminSettlementPage() {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [triggeringId, setTriggeringId] = useState(null);

  useEffect(() => {
    const loadData = async () => {
      setLoading(true);
      setError("");
      try {
        const result = await getSettlementMonitor(0, 50);
        setData(result);
      } catch (err) {
        setError("Failed to load settlement data");
        console.error(err);
      } finally {
        setLoading(false);
      }
    };

    loadData();
  }, []);

  const handleTriggerSettlement = async (transactionId) => {
    if (!window.confirm("Manually trigger settlement for this transaction?")) return;

    setTriggeringId(transactionId);
    try {
      await triggerSettlement(transactionId);
      alert("Settlement triggered successfully");
      window.location.reload();
    } catch (err) {
      alert(err?.response?.data?.message || "Failed to trigger settlement");
    } finally {
      setTriggeringId(null);
    }
  };

  return (
    <AdminLayout>
      <div className="admin-list">
        <h1>⏳ Settlement Monitor</h1>

        {error && <div className="alert alert-error">{error}</div>}

        {loading ? (
          <div style={{ padding: "2rem" }}>Loading settlement data...</div>
        ) : data ? (
          <>
            <div style={{
              display: "grid",
              gridTemplateColumns: "repeat(auto-fit, minmax(150px, 1fr))",
              gap: "1rem",
              marginBottom: "2rem",
            }}>
              <div style={{
                background: "white",
                padding: "1rem",
                borderRadius: "8px",
                boxShadow: "0 1px 3px rgba(0, 0, 0, 0.1)",
              }}>
                <small style={{ color: "#6b7280" }}>Due Today</small>
                <p style={{ margin: "0.5rem 0 0 0", fontSize: "1.5rem", fontWeight: "700" }}>
                  {data.dueTodayCount}
                </p>
              </div>
              <div style={{
                background: "white",
                padding: "1rem",
                borderRadius: "8px",
                boxShadow: "0 1px 3px rgba(0, 0, 0, 0.1)",
              }}>
                <small style={{ color: "#6b7280" }}>Due Tomorrow</small>
                <p style={{ margin: "0.5rem 0 0 0", fontSize: "1.5rem", fontWeight: "700" }}>
                  {data.dueTomorrowCount}
                </p>
              </div>
              <div style={{
                background: "white",
                padding: "1rem",
                borderRadius: "8px",
                boxShadow: "0 1px 3px rgba(0, 0, 0, 0.1)",
              }}>
                <small style={{ color: "#6b7280" }}>Overdue</small>
                <p style={{ margin: "0.5rem 0 0 0", fontSize: "1.5rem", fontWeight: "700", color: "#ef4444" }}>
                  {data.overdueCount}
                </p>
              </div>
            </div>

            <div className="list-card">
              <table className="admin-table">
                <thead>
                  <tr>
                    <th>From</th>
                    <th>To</th>
                    <th>Amount</th>
                    <th>Due Date</th>
                    <th>Days Until Due</th>
                    <th>Action</th>
                  </tr>
                </thead>
                <tbody>
                  {data.pendingTransactions && data.pendingTransactions.map((tx) => (
                    <tr key={tx.transactionId}>
                      <td>{tx.senderEmail}</td>
                      <td>{tx.receiverEmail}</td>
                      <td>{formatINR(tx.amount)}</td>
                      <td>{tx.settlementDueDate}</td>
                      <td>{tx.daysUntilDue}</td>
                      <td>
                        <button
                          onClick={() => handleTriggerSettlement(tx.transactionId)}
                          disabled={triggeringId === tx.transactionId}
                          className="btn btn-small"
                          style={{ fontSize: "0.75rem" }}
                        >
                          {triggeringId === tx.transactionId ? "..." : "Settle"}
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </>
        ) : null}
      </div>
    </AdminLayout>
  );
}
