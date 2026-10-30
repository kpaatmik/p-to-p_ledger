/**
 * AdminReconciliationPage — reconciliation and account balancing.
 */

import { useEffect, useState } from "react";
import AdminLayout from "../../components/AdminLayout.jsx";
import { getReconciliation } from "../../api/adminService.js";
import { formatINR } from "../../utils/currency.js";

export default function AdminReconciliationPage() {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    const loadData = async () => {
      setLoading(true);
      setError("");
      try {
        const result = await getReconciliation();
        setData(result);
      } catch (err) {
        setError("Failed to load reconciliation data");
        console.error(err);
      } finally {
        setLoading(false);
      }
    };

    loadData();
  }, []);

  return (
    <AdminLayout>
      <div style={{ padding: "2rem" }}>
        <h1>📋 Reconciliation</h1>

        {error && <div className="alert alert-error">{error}</div>}

        {loading ? (
          <div>Loading reconciliation data...</div>
        ) : data ? (
          <div style={{ maxWidth: "600px" }}>
            <div style={{
              background: "white",
              borderRadius: "8px",
              padding: "2rem",
              boxShadow: "0 1px 3px rgba(0, 0, 0, 0.1)",
            }}>
              <div style={{ marginBottom: "1.5rem" }}>
                <small style={{ color: "#6b7280" }}>Total Debits (Settled)</small>
                <p style={{ margin: "0.5rem 0 0 0", fontSize: "1.5rem", fontWeight: "700" }}>
                  {formatINR(data.totalSettledDebits)}
                </p>
              </div>

              <div style={{ marginBottom: "1.5rem" }}>
                <small style={{ color: "#6b7280" }}>Total Credits (Settled)</small>
                <p style={{ margin: "0.5rem 0 0 0", fontSize: "1.5rem", fontWeight: "700" }}>
                  {formatINR(data.totalSettledCredits)}
                </p>
              </div>

              <div style={{ marginBottom: "1.5rem" }}>
                <small style={{ color: "#6b7280" }}>Pending Settlement</small>
                <p style={{ margin: "0.5rem 0 0 0", fontSize: "1.5rem", fontWeight: "700" }}>
                  {formatINR(data.pendingAmount)}
                </p>
              </div>

              <hr style={{ margin: "1.5rem 0" }} />

              <div style={{
                padding: "1rem",
                borderRadius: "4px",
                backgroundColor: data.matched ? "#d1fae5" : "#fee2e2",
                color: data.matched ? "#065f46" : "#991b1b",
              }}>
                {data.matched ? "✅ Balanced" : "⚠️ Mismatch Detected"}
              </div>

              {!data.matched && (
                <div style={{
                  marginTop: "1rem",
                  padding: "1rem",
                  backgroundColor: "#fef3c7",
                  borderLeft: "4px solid #f59e0b",
                  color: "#92400e",
                  fontSize: "0.875rem",
                }}>
                  <strong>Difference:</strong> {formatINR(data.difference)}
                </div>
              )}
            </div>
          </div>
        ) : null}
      </div>
    </AdminLayout>
  );
}
