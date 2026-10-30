/**
 * SendMoneyPage — P2P transfer flow.
 *
 * Backend requires recipientId (UUID), not email.
 * Flow:
 *   1. User types email/phone
 *   2. We call GET /users/search?q=... to resolve the UUID
 *   3. User confirms the matched recipient name
 *   4. User enters amount and remark
 *   5. PIN confirmation dialog
 *   6. POST /transfer with { recipientId, amount, transactionPin, requestId }
 */

import { useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import Layout from "../components/Layout.jsx";
import PinConfirmDialog from "../components/PinConfirmDialog.jsx";
import { searchUser, initiateTransfer } from "../api/transferService.js";
import { isValidAmount } from "../utils/validation.js";
import { formatINR } from "../utils/currency.js";
import ROUTES from "../constants/routes.js";
import "../styles/forms.css";

function generateRequestId() {
  return `req-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
}

export default function SendMoneyPage() {
  const navigate = useNavigate();

  // Recipient search
  const [searchQuery, setSearchQuery] = useState("");
  const [searching, setSearching] = useState(false);
  const [recipient, setRecipient] = useState(null); // { userId, name, maskedEmail }
  const [searchError, setSearchError] = useState("");

  // Transfer form
  const [amount, setAmount] = useState("");
  const [remark, setRemark] = useState("");
  const [errors, setErrors] = useState({});

  // Transfer state
  const [loading, setLoading] = useState(false);
  const [showPinDialog, setShowPinDialog] = useState(false);
  const [result, setResult] = useState(null);

  // requestId is stable for the current transfer attempt; regenerated on "Send Another"
  const requestIdRef = useRef(generateRequestId());

  // ── Step 1: search for recipient ──────────────────────────────────────────

  const handleSearch = async (e) => {
    e.preventDefault();
    setSearchError("");
    setRecipient(null);

    if (!searchQuery.trim()) {
      setSearchError("Enter an email or phone number");
      return;
    }

    setSearching(true);
    try {
      const user = await searchUser(searchQuery.trim());
      setRecipient(user);
    } catch (err) {
      const msg = err?.response?.data?.message;
      if (err?.response?.status === 404) {
        setSearchError("No user found with that email or phone");
      } else {
        setSearchError(msg || "Search failed");
      }
    } finally {
      setSearching(false);
    }
  };

  const handleClearRecipient = () => {
    setRecipient(null);
    setSearchQuery("");
    setSearchError("");
    setAmount("");
    setErrors({});
  };

  // ── Step 2: validate and show PIN dialog ─────────────────────────────────

  const handleSubmit = (e) => {
    e.preventDefault();
    setErrors({});

    const newErrors = {};
    if (!recipient) newErrors.recipient = "Select a recipient first";
    if (!isValidAmount(amount)) newErrors.amount = "Amount must be greater than 0";

    if (Object.keys(newErrors).length > 0) {
      setErrors(newErrors);
      return;
    }

    setShowPinDialog(true);
  };

  // ── Step 3: submit transfer with PIN ─────────────────────────────────────

  const handleTransferConfirm = async (pin) => {
    setLoading(true);
    try {
      const data = await initiateTransfer({
        recipientId: recipient.userId,
        amount,
        transactionPin: pin,
        requestId: requestIdRef.current,
      });
      setResult(data);
      setShowPinDialog(false);
    } catch (err) {
      const msg = err?.response?.data?.message || "Transfer failed";
      setErrors({ submit: msg });
      setShowPinDialog(false);
    } finally {
      setLoading(false);
    }
  };

  // ── Success screen ────────────────────────────────────────────────────────

  if (result) {
    return (
      <Layout>
        <div className="form-page">
          <div className="success-state">
            <h2>✅ Transfer Initiated</h2>
            <div className="result-box">
              <div className="result-row">
                <span className="result-label">Recipient:</span>
                <span className="result-value">{result.recipientEmail}</span>
              </div>
              <div className="result-row">
                <span className="result-label">Amount:</span>
                <span className="result-value">{formatINR(result.amount)}</span>
              </div>
              <div className="result-row">
                <span className="result-label">Status:</span>
                <span
                  className={`status-badge ${result.status.toLowerCase()}`}
                >
                  {result.status}
                </span>
              </div>
              <div className="result-row">
                <span className="result-label">Settlement Due:</span>
                <span className="result-value">
                  {result.settlementDueDate}{" "}
                  {result.daysUntilSettlement != null
                    ? `(${result.daysUntilSettlement} day${result.daysUntilSettlement !== 1 ? "s" : ""})`
                    : ""}
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
                onClick={() => navigate(ROUTES.TRANSACTIONS)}
              >
                View Transactions
              </button>
              <button
                className="btn btn-primary"
                onClick={() => {
                  setResult(null);
                  setRecipient(null);
                  setSearchQuery("");
                  setAmount("");
                  setRemark("");
                  requestIdRef.current = generateRequestId();
                }}
              >
                Send Another
              </button>
            </div>
          </div>
        </div>
      </Layout>
    );
  }

  // ── Main form ─────────────────────────────────────────────────────────────

  return (
    <Layout>
      <div className="form-page">
        <h1>💸 Send Money</h1>
        <div className="form-card">
          {/* Recipient search */}
          {!recipient ? (
            <div className="form-section">
              <h2 style={{ fontSize: "1rem", marginBottom: "1rem" }}>
                Find recipient
              </h2>
              <form onSubmit={handleSearch}>
                <div className="form-group">
                  <label htmlFor="searchQuery">Email or phone number</label>
                  <div style={{ display: "flex", gap: "0.5rem" }}>
                    <input
                      id="searchQuery"
                      type="text"
                      placeholder="user@example.com or 9876543210"
                      value={searchQuery}
                      onChange={(e) => setSearchQuery(e.target.value)}
                      disabled={searching}
                    />
                    <button
                      type="submit"
                      className="btn btn-primary"
                      disabled={searching || !searchQuery.trim()}
                      style={{ whiteSpace: "nowrap" }}
                    >
                      {searching ? "Searching..." : "Find"}
                    </button>
                  </div>
                  {searchError && (
                    <span className="error">{searchError}</span>
                  )}
                </div>
              </form>
            </div>
          ) : (
            /* Recipient confirmed — show summary and transfer form */
            <form onSubmit={handleSubmit}>
              {/* Confirmed recipient box */}
              <div className="info-box" style={{ marginBottom: "1.5rem" }}>
                <strong>
                  ✔ Sending to: {recipient.name || recipient.maskedEmail}
                </strong>
                <span style={{ marginLeft: "0.5rem", color: "#1e40af" }}>
                  {recipient.maskedEmail}
                </span>
                <button
                  type="button"
                  onClick={handleClearRecipient}
                  style={{
                    marginLeft: "1rem",
                    background: "none",
                    border: "none",
                    color: "#ef4444",
                    cursor: "pointer",
                    fontSize: "0.875rem",
                  }}
                >
                  Change
                </button>
                {errors.recipient && (
                  <span className="error">{errors.recipient}</span>
                )}
              </div>

              <div className="form-group">
                <label htmlFor="amount">Amount (₹)</label>
                <input
                  id="amount"
                  type="number"
                  step="0.01"
                  placeholder="0.00"
                  value={amount}
                  onChange={(e) => setAmount(e.target.value)}
                  disabled={loading}
                />
                {errors.amount && (
                  <span className="error">{errors.amount}</span>
                )}
              </div>

              <div className="form-group">
                <label htmlFor="remark">Note (optional)</label>
                <textarea
                  id="remark"
                  placeholder="Add a note..."
                  value={remark}
                  onChange={(e) => setRemark(e.target.value)}
                  disabled={loading}
                  rows="2"
                />
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
                  onClick={() => navigate(ROUTES.DASHBOARD)}
                  disabled={loading}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="btn btn-primary"
                  disabled={loading || !amount}
                >
                  {loading ? "Processing..." : "Continue to PIN"}
                </button>
              </div>
            </form>
          )}
        </div>
      </div>

      {showPinDialog && (
        <PinConfirmDialog
          onConfirm={handleTransferConfirm}
          onCancel={() => setShowPinDialog(false)}
          loading={loading}
        />
      )}
    </Layout>
  );
}
