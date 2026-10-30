/**
 * PIN Confirmation Dialog — prompts user for transaction PIN before payment.
 */

import { useState } from "react";
import { isValidPin } from "../utils/validation.js";
import "../styles/dialog.css";

export default function PinConfirmDialog({ onConfirm, onCancel, loading }) {
  const [pin, setPin] = useState("");
  const [error, setError] = useState("");

  const handleSubmit = (e) => {
    e.preventDefault();
    setError("");

    if (!isValidPin(pin)) {
      setError("PIN must be exactly 4 digits");
      return;
    }

    onConfirm(pin);
  };

  const handleCancel = () => {
    setPin("");
    setError("");
    onCancel();
  };

  return (
    <div className="dialog-overlay" onClick={handleCancel}>
      <div className="dialog-content" onClick={(e) => e.stopPropagation()}>
        <h2>Confirm Transaction</h2>
        <p>Please enter your 4-digit PIN to continue</p>
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label htmlFor="pin">Transaction PIN</label>
            <input
              id="pin"
              type="password"
              inputMode="numeric"
              maxLength="4"
              placeholder="****"
              value={pin}
              onChange={(e) => setPin(e.target.value)}
              disabled={loading}
              autoFocus
            />
            {error && <span className="error">{error}</span>}
          </div>
          <div className="dialog-actions">
            <button
              type="button"
              onClick={handleCancel}
              className="btn btn-secondary"
              disabled={loading}
            >
              Cancel
            </button>
            <button
              type="submit"
              className="btn btn-primary"
              disabled={loading || !pin}
            >
              {loading ? "Processing..." : "Confirm"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
