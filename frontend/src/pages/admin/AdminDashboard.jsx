/**
 * AdminDashboard — admin dashboard with metrics and charts.
 */

import { useEffect, useState } from "react";
import AdminLayout from "../../components/AdminLayout.jsx";
import { getAdminDashboard } from "../../api/adminService.js";
import { formatINR } from "../../utils/currency.js";
import "../../styles/admin-dashboard.css";

export default function AdminDashboard() {
  const [dashboard, setDashboard] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    const loadDashboard = async () => {
      setLoading(true);
      setError("");
      try {
        const data = await getAdminDashboard();
        setDashboard(data);
      } catch (err) {
        setError("Failed to load dashboard");
        console.error(err);
      } finally {
        setLoading(false);
      }
    };

    loadDashboard();
  }, []);

  if (loading) {
    return (
      <AdminLayout>
        <div style={{ padding: "2rem" }}>Loading dashboard...</div>
      </AdminLayout>
    );
  }

  return (
    <AdminLayout>
      <div className="admin-dashboard">
        <h1>📊 Admin Dashboard</h1>

        {error && <div className="alert alert-error">{error}</div>}

        {dashboard && (
          <>
            <div className="metrics-grid">
              <div className="metric-card">
                <h3>Total Users</h3>
                <p className="metric-value">{dashboard.totalUsers}</p>
              </div>
              <div className="metric-card">
                <h3>Today Initiated</h3>
                <p className="metric-value">{dashboard.initiatedCount}</p>
                <small>{formatINR(dashboard.initiatedAmount)}</small>
              </div>
              <div className="metric-card">
                <h3>Today Settled</h3>
                <p className="metric-value">{dashboard.settledCount}</p>
                <small>{formatINR(dashboard.settledAmount)}</small>
              </div>
              <div className="metric-card">
                <h3>Pending Settlement</h3>
                <p className="metric-value">{formatINR(dashboard.pendingSettlementAmount)}</p>
              </div>
              <div className="metric-card alert-card">
                <h3>Fraud Alerts</h3>
                <p className="metric-value">{dashboard.pendingFraudAlerts}</p>
              </div>
            </div>

            {dashboard.dailyVolumesLast30Days && dashboard.dailyVolumesLast30Days.length > 0 && (
              <div className="chart-card">
                <h3>30-Day Transaction Volume</h3>
                <div className="simple-chart">
                  {dashboard.dailyVolumesLast30Days.map((day, idx) => {
                    const maxAmount = Math.max(
                      ...dashboard.dailyVolumesLast30Days.map((d) => parseFloat(d.amount || 0))
                    );
                    const height = maxAmount > 0 ? (parseFloat(day.amount || 0) / maxAmount) * 100 : 0;
                    return (
                      <div key={idx} className="chart-bar" title={`${day.date}: ${formatINR(day.amount)}`}>
                        <div className="bar" style={{ height: `${height}%` }}></div>
                        <small>{new Date(day.date).getDate()}</small>
                      </div>
                    );
                  })}
                </div>
              </div>
            )}
          </>
        )}
      </div>
    </AdminLayout>
  );
}
