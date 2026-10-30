/**
 * CreateReservationPage — create a Check Please reservation.
 *
 * Key correctness notes:
 *   - expiresAt must be sent as ISO-8601 Instant (e.g. "2025-12-31T18:30:00.000Z"),
 *     NOT a LocalDate or localdatetime without timezone.
 *   - The backend validates: min 1 hour, max 30 days from now.
 *   - merchantId must be the merchant UUID, not its name.
 *   - Merchants are loaded dynamically from GET /merchants.
 */

import { useEffect, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import Layout from "../components/Layout.jsx";
import { createReservation, getMerchants } from "../api/merchantService.js";
import ROUTES from "../constants/routes.js";
import { formatINR } from "../utils/currency.js";
import { isValidAmount } from "../utils/validation.js";
import "../styles/forms.css";

export default function CreateReservationPage() {
  const location = useLocation();
  const navigate = useNavigate();

  const [merchants, setMerchants] = useState([]);
  const [merchantsLoading, setMerchantsLoading] = useState(true);

  // Pre-select if merchant was passed from MerchantListPage
  const [merchantId, setMerchantId] = useState(
    location.state?.merchantId || ""
  );
  const [maxAmount, setMaxAmount] = useState("");
  // datetime-local value (no timezone) — we convert to Instant on submit
  const [expiresLocal, setExpiresLocal] = useState("");

  const [errors, setErrors] = useState({});
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState(null);

  // Load ACTIVE merchants for dropdown
  useEffect(() => {
    const loadMerchants = async () => {
      try {
        const data = await getMerchants();
        setMerchants(Array.isArray(data) ? data : []);
      } catch (err) {
        console.error("Failed to load merchants:", err);
      } finally {
        setMerchantsLoading(false);
      }
    };
    loadMerchants();
  }, []);

  const validate = () => {
    const newErrors = {};
    if (!merchantId) newErrors.merchantId = "Select a merchant";
    if (!isValidAmount(maxAmount))
      newErrors.maxAmount = "Amount must be greater than 0";
    if (!expiresLocal) {
      newErrors.expiresAt = "Select an expiry date and time";
    } else {
      const expiresInstant = new Date(expiresLocal);
      const now = new Date();
      const minExpiry = new Date(now.getTime() + 60 * 60 * 1000); // +1 hour
      const maxExpiry = new Date(now.getTime() + 30 * 24 * 60 * 60 * 1000); // +30 days
      if (expiresInstant <= minExpiry) {
        newErrors.expiresAt = "Expiry must be at least 1 hour from now";
      } else if (expiresInstant > maxExpiry) {
        newErrors.expiresAt = "Expiry cannot be more than 30 days from now";
      }
    }
    return newErrors;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErrors({});

    const newErrors = validate();
    if (Object.keys(newErrors).length > 0) {
      setErrors(newErrors);
      return;
    }

    setLoading(true);
    try {
      // Convert datetime-local string to ISO-8601 Instant (UTC)
      const expiresAt = new Date(expiresLocal).toISOString();

      const data = await createReservation({
        merchantId,
        maxAmount,
        expiresAt, // e.g. "2025-12-31T18:30:00.000Z"
      });
      setResult(data);
    } catch (err) {
      setErrors({
        submit:
          err?.response?.data?.message || "Failed to create reservation",
      });
    } finally {
      setLoading(false);
    }
  };

  // Success screen
  if (result) {
    return (
      <Layout>
        <div className="form-page">
          <div className="success-state">
            <h2>✅ Reservation Created</h2>
            <div className="result-box">
              <div className="result-row">
                <span className="result-label">Merchant:</span>
                <span className="result-value">{result.merchantName}</span>
              </div>
              <div className="result-row">
                <span className="result-label">Max Amount:</span>
                <span className="result-value">
                  {formatINR(result.maxAmount)}
                </span>
              </div>
              <div className="result-row">
                <span className="result-label">Expires:</span>
                <span className="result-value">
                  {new Date(result.expiresAt).toLocaleString()}
                </span>
              </div>
              <div className="result-row">
                <span className="result-label">Status:</span>
                <span className="status-badge active">{result.status}</span>
              </div>
            </div>
            <button
              className="btn btn-primary"
              onClick={() => navigate(ROUTES.RESERVATIONS)}
              style={{ marginTop: "1.5rem" }}
            >
              View Reservations
            </button>
          </div>
        </div>
      </Layout>
    );
  }

  // Set min/max for the datetime-local input
  const nowIso = new Date(Date.now() + 60 * 60 * 1000)
    .toISOString()
    .slice(0, 16);
  const maxIso = new Date(Date.now() + 30 * 24 * 60 * 60 * 1000)
    .toISOString()
    .slice(0, 16);

  return (
    <Layout>
      <div className="form-page">
        <h1>📝 Create Reservation</h1>
        <div className="form-card">
          <form onSubmit={handleSubmit}>
            <div className="form-group">
              <label htmlFor="merchantId">Merchant</label>
              <select
                id="merchantId"
                value={merchantId}
                onChange={(e) => setMerchantId(e.target.value)}
                disabled={loading || merchantsLoading}
              >
                <option value="">
                  {merchantsLoading ? "Loading..." : "Select a merchant..."}
                </option>
                {merchants.map((m) => (
                  <option key={m.id} value={m.id}>
                    {m.name}
                  </option>
                ))}
              </select>
              {errors.merchantId && (
                <span className="error">{errors.merchantId}</span>
              )}
            </div>

            <div className="form-group">
              <label htmlFor="maxAmount">Maximum Amount (₹)</label>
              <input
                id="maxAmount"
                type="number"
                step="0.01"
                min="0.01"
                placeholder="0.00"
                value={maxAmount}
                onChange={(e) => setMaxAmount(e.target.value)}
                disabled={loading}
              />
              {errors.maxAmount && (
                <span className="error">{errors.maxAmount}</span>
              )}
            </div>

            <div className="form-group">
              <label htmlFor="expiresAt">Expiry Date &amp; Time</label>
              <input
                id="expiresAt"
                type="datetime-local"
                min={nowIso}
                max={maxIso}
                value={expiresLocal}
                onChange={(e) => setExpiresLocal(e.target.value)}
                disabled={loading}
              />
              {errors.expiresAt && (
                <span className="error">{errors.expiresAt}</span>
              )}
              <small>Between 1 hour and 30 days from now</small>
            </div>

            {errors.submit && (
              <div className="alert alert-error" style={{ marginBottom: "1rem" }}>
                {errors.submit}
              </div>
            )}

            <div className="form-actions">
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => navigate(ROUTES.MERCHANTS)}
                disabled={loading}
              >
                Cancel
              </button>
              <button
                type="submit"
                className="btn btn-primary"
                disabled={loading || merchantsLoading}
              >
                {loading ? "Creating..." : "Create Reservation"}
              </button>
            </div>
          </form>
        </div>
      </div>
    </Layout>
  );
}
