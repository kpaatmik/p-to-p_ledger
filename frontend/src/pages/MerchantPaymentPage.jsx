/**
 * MerchantPaymentPage — capture a merchant payment against an ACTIVE reservation.
 *
 * POST /merchant-payments
 * Body: { requestId, reservationId, amount, transactionPin }
 * Response 201: MerchantPaymentResponse {
 *   paymentId, reservationId, amount, paymentStatus,
 *   walletTotalBalance, walletAvailableBalance, walletReservedBalance,
 *   reservationRemainingAmount, message
 * }
 */

import { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import Layout from "../components/Layout.jsx";
import PinConfirmDialog from "../components/PinConfirmDialog.jsx";
import { getReservations, captureMerchantPayment } from "../api/merchantService.js";
import { formatINR } from "../utils/currency.js";
import { isValidAmount } from "../utils/validation.js";
import ROUTES from "../constants/routes.js";
import "../styles/forms.css";

function generateRequestId() {
  return `pay-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
}

export default function MerchantPaymentPage() {
  const navigate = useNavigate();

  const [reservations, setReservations] = useState([]);
  const [reservationsLoading, setReservationsLoading] = useState(true);

  const [selectedReservation, setSelectedReservation] = useState(null);
  const [amount, setAmount] = useState("");
  const [errors, setErrors] = useState({});

  const [loading, setLoading] = useState(false);
  const [showPinDialog, setShowPinDialog] = useState(false);
  const [result, setResult] = useState(null);

  const requestIdRef = useRef(generateRequestId());

  // Load ACTIVE reservations
  useEffect(() => {
    const load = async () => {
      try {
        const data = await getReservations("ACTIVE");
        setReservations(Array.isArray(data) ? data : []);
      } catch (err) {
        console.error("Failed to load reservations:", err);
      } finally {
        setReservationsLoading(false);
      }
    };
    load();
  }, []);

  const handleReservationSelect = (e) => {
    const id = e.target.value;
    const found = reservations.find((r) => r.id === id) || null;
    setSelectedReservation(found);
    setAmount("");
    setErrors({});
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    setErrors({});

    const newErrors = {};
    if (!selectedReservation) newErrors.reservation = "Select a reservation";
    if (!isValidAmount(amount)) {
      newErrors.amount = "Amount must be greater than 0";
    } else if (
      selectedReservation &&
      parseFloat(amount) > parseFloat(selectedReservation.remainingAmount)
    ) {
      newErrors.amount = `Cannot exceed remaining amount of ${formatINR(selectedReservation.remainingAmount)}`;
    }

    if (Object.keys(newErrors).length > 0) {
      setErrors(newErrors);
      return;
    }

    setShowPinDialog(true);
  };

  const handlePayConfirm = async (pin) => {
    setLoading(true);
    try {
      const data = await captureMerchantPayment({
        requestId: requestIdRef.current,
        reservationId: selectedReservation.id,
        amount,
        transactionPin: pin,
      });
      setResult(data);
      setShowPinDialog(false);
    } catch (err) {
      const msg = err?.response?.data?.message || "Payment failed";
      setErrors({ submit: msg });
      setShowPinDialog(false);
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
            <h2>
              {result.paymentStatus === "CAPTURED"
                ? "✅ Payment Captured"
                : "❌ Payment Failed"}
            </h2>
            {result.message && (
              <p style={{ color: "#6b7280" }}>{result.message}</p>
            )}
            <div className="result-box">
              <div className="result-row">
                <span className="result-label">Captured:</span>
                <span className="result-value">{formatINR(result.amount)}</span>
              </div>
              <div className="result-row">
                <span className="result-label">Remaining on reservation:</span>
                <span className="result-value">
                  {formatINR(result.reservationRemainingAmount)}
                </span>
              </div>
              <div className="result-row">
                <span className="result-label">Wallet Available:</span>
                <span className="result-value">
                  {formatINR(result.walletAvailableBalance)}
                </span>
              </div>
            </div>
            <div
              style={{
                display: "flex",
                gap: "1rem",
                justifyContent: "center",
                marginTop: "1.5rem",
              }}
            >
              <button
                className="btn btn-secondary"
                onClick={() => navigate(ROUTES.RESERVATIONS)}
              >
                View Reservations
              </button>
              <button
                className="btn btn-primary"
                onClick={() => {
                  setResult(null);
                  setSelectedReservation(null);
                  setAmount("");
                  requestIdRef.current = generateRequestId();
                }}
              >
                Make Another Payment
              </button>
            </div>
          </div>
        </div>
      </Layout>
    );
  }

  return (
    <Layout>
      <div className="form-page">
        <h1>🏪 Merchant Payment</h1>
        <div className="form-card">
          <form onSubmit={handleSubmit}>
            <div className="form-group">
              <label htmlFor="reservation">Reservation</label>
              <select
                id="reservation"
                value={selectedReservation?.id || ""}
                onChange={handleReservationSelect}
                disabled={loading || reservationsLoading}
              >
                <option value="">
                  {reservationsLoading
                    ? "Loading..."
                    : reservations.length === 0
                    ? "No active reservations"
                    : "Select a reservation..."}
                </option>
                {reservations.map((r) => (
                  <option key={r.id} value={r.id}>
                    {r.merchantName} — up to {formatINR(r.remainingAmount)}
                  </option>
                ))}
              </select>
              {errors.reservation && (
                <span className="error">{errors.reservation}</span>
              )}
            </div>

            {selectedReservation && (
              <div className="info-box">
                <strong>Merchant:</strong> {selectedReservation.merchantName}
                <br />
                <strong>Remaining:</strong>{" "}
                {formatINR(selectedReservation.remainingAmount)}
              </div>
            )}

            <div className="form-group">
              <label htmlFor="amount">Payment Amount (₹)</label>
              <input
                id="amount"
                type="number"
                step="0.01"
                min="0.01"
                max={selectedReservation?.remainingAmount || undefined}
                placeholder="0.00"
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
                disabled={loading || !selectedReservation}
              />
              {errors.amount && (
                <span className="error">{errors.amount}</span>
              )}
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
                onClick={() => navigate(ROUTES.RESERVATIONS)}
                disabled={loading}
              >
                Cancel
              </button>
              <button
                type="submit"
                className="btn btn-primary"
                disabled={loading || !selectedReservation || !amount}
              >
                {loading ? "Processing..." : "Pay"}
              </button>
            </div>
          </form>
        </div>
      </div>

      {showPinDialog && (
        <PinConfirmDialog
          onConfirm={handlePayConfirm}
          onCancel={() => setShowPinDialog(false)}
          loading={loading}
        />
      )}
    </Layout>
  );
}
