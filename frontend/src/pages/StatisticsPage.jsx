/**
 * StatisticsPage — user's transaction statistics.
 */

import { useEffect, useState } from "react";
import Layout from "../components/Layout.jsx";
import { getStatistics } from "../api/transferService.js";
import { formatINR } from "../utils/currency.js";
import "../styles/statistics.css";

export default function StatisticsPage() {
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    const loadStats = async () => {
      setLoading(true);
      setError("");
      try {
        const data = await getStatistics();
        setStats(data);
      } catch (err) {
        setError("Failed to load statistics");
        console.error(err);
      } finally {
        setLoading(false);
      }
    };

    loadStats();
  }, []);

  if (loading) {
    return (
      <Layout>
        <div className="loading-state">Loading statistics...</div>
      </Layout>
    );
  }

  return (
    <Layout>
      <div className="statistics-page">
        <h1>📈 Your Statistics</h1>

        {error && <div className="alert alert-error">{error}</div>}

        {stats && (
          <div className="stats-grid">
            <div className="stat-card">
              <div className="stat-icon">📤</div>
              <div className="stat-content">
                <h3>Total Sent</h3>
                <p className="stat-value">{formatINR(stats.totalSent)}</p>
              </div>
            </div>

            <div className="stat-card">
              <div className="stat-icon">📥</div>
              <div className="stat-content">
                <h3>Total Received</h3>
                <p className="stat-value">{formatINR(stats.totalReceived)}</p>
              </div>
            </div>

            <div className="stat-card">
              <div className="stat-icon">📊</div>
              <div className="stat-content">
                <h3>This Month</h3>
                <p className="stat-value">{stats.monthlyTransactionCount} transactions</p>
              </div>
            </div>
          </div>
        )}
      </div>
    </Layout>
  );
}
