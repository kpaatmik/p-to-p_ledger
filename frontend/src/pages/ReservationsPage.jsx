/**
 * ReservationsPage — list of Check Please reservations with cancellation.
 *
 * NOTE: GET /reservations returns List<ReservationResponse> (plain array),
 * NOT a Spring Page — there is no .content wrapper, no totalPages.
 */

import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import Layout from "../components/Layout.jsx";
import { getReservations, cancelReservation } from "../api/merchantService.js";
import ROUTES from "../constants/routes.js";
import { formatINR } from "../utils/currency.js";
import { formatDate } from "../utils/date.js";
import "../styles/list-page.css";

const STATUS_LABELS = ["", "ACTIVE", "CONSUMED", "CANCELLED", "EXPIRED"];

export default function ReservationsPage() {
  const navigate = useNavigate();
  const [reservations, setReservations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [statusFilter, setStatusFilter] = useState("");
  const [cancelingId, setCancelingId] = useState(null);

  const loadReservations = async (status) => {
    setLoading(true);
    setError("");
    try {
      // Returns a plain array — no .content
      const data = await getReservations(status || null);
      setReservations(Array.isArray(data) ? data : []);
    } catch (err) {
      setError("Failed to load reservations");
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadReservations(statusFilter);
  }, [statusFilter]);

  const handleCancelReservation = async (id) => {
    if (!window.confirm("Are you sure you want to cancel this reservation?"))
      return;

    setCancelingId(id);
    try {
      await cancelReservation(id);
      // Remove from local list immediately
      setReservations((prev) => prev.filter((r) => r.id !== id));
    } catch (err) {
      alert(err?.response?.data?.message || "Failed to cancel reservation");
    } finally {
      setCancelingId(null);
    }
  };

  return (
    <Layout>
      <div className="list-page">
        <div
          style={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
            marginBottom: "2rem",
          }}
        >
          <h1>📋 Reservations</h1>
          <button
            className="btn btn-primary"
            onClick={() => navigate(ROUTES.MERCHANTS)}
          >
            + New Reservation
          </button>
        </div>

        {/* Status filter */}
        <div className="filters-card" style={{ marginBottom: "2rem" }}>
          <div className="filter-group">
            <label htmlFor="statusFilter">Status</label>
            <select
              id="statusFilter"
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
            >
              <option value="">All</option>
              <option value="ACTIVE">Active</option>
              <option value="CONSUMED">Consumed</option>
              <option value="CANCELLED">Cancelled</option>
              <option value="EXPIRED">Expired</option>
            </select>
          </div>
        </div>

        {error && <div className="alert alert-error">{error}</div>}

        {loading ? (
          <div className="loading-state">Loading reservations...</div>
        ) : reservations.length > 0 ? (
          <div className="list-card">
            <table className="transaction-table">
              <thead>
                <tr>
                  <th>Merchant</th>
                  <th>Max Amount</th>
                  <th>Remaining</th>
                  <th>Status</th>
                  <th>Expires</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {reservations.map((res) => (
                  <tr key={res.id}>
                    <td>{res.merchantName}</td>
                    <td>{formatINR(res.maxAmount)}</td>
                    <td>{formatINR(res.remainingAmount)}</td>
                    <td>
                      <span
                        className={`status-badge ${res.status.toLowerCase()}`}
                      >
                        {res.status}
                      </span>
                    </td>
                    <td>{formatDate(res.expiresAt)}</td>
                    <td>
                      {res.status === "ACTIVE" && (
                        <button
                          className="btn btn-small"
                          onClick={() => handleCancelReservation(res.id)}
                          disabled={cancelingId === res.id}
                          style={{ color: "#ef4444" }}
                        >
                          {cancelingId === res.id ? "Cancelling..." : "Cancel"}
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <div className="empty-state">
            No reservations found
            {statusFilter && ` for status "${statusFilter}"`}
          </div>
        )}
      </div>
    </Layout>
  );
}
