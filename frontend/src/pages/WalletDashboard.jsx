/**
 * WalletDashboard — main user dashboard showing wallet balance and recent transactions.
 */

import { useEffect, useState } from "react";
import { useAuth } from "../contexts/AuthContext.jsx";
import Layout from "../components/Layout.jsx";
import WalletCard from "../components/WalletCard.jsx";
import MiniStatement from "../components/MiniStatement.jsx";
import PinConfirmDialog from "../components/PinConfirmDialog.jsx";
import { getWallet, loadMoney } from "../api/walletService.js";
import { formatINR } from "../utils/currency.js";
import { isValidAmount } from "../utils/validation.js";
import "../styles/dashboard.css";

export default function WalletDashboard() {
  const { currentUser } = useAuth();
  const [wallet, setWallet] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [loadAmount, setLoadAmount] = useState("");
  const [loadError, setLoadError] = useState("");
  const [loadSuccess, setLoadSuccess] = useState("");
  const [loadingMoney, setLoadingMoney] = useState(false);
  const [showPinDialog, setShowPinDialog] = useState(false);

  useEffect(() => {
    const fetchWallet = async () => {
      setLoading(true);
      setError("");
      try {
        const data = await getWallet();
        setWallet(data);
      } catch (err) {
        setError("Failed to load wallet information");
        console.error(err);
      } finally {
        setLoading(false);
      }
    };

    fetchWallet();
  }, []);

  const handleLoadMoneyClick = () => {
    setLoadError("");
    if (!isValidAmount(loadAmount)) {
      setLoadError("Please enter a valid amount greater than 0");
      return;
    }
    setShowPinDialog(true);
  };

  const handleLoadMoneyConfirm = async (pin) => {
    setLoadingMoney(true);
    setLoadError("");
    try {
      const data = await loadMoney(loadAmount);
      setWallet(data);
      setLoadSuccess(`Successfully loaded ${formatINR(loadAmount)}`);
      setLoadAmount("");
      setShowPinDialog(false);
      setTimeout(() => setLoadSuccess(""), 3000);
    } catch (err) {
      setLoadError(err?.response?.data?.message || "Failed to load money");
    } finally {
      setLoadingMoney(false);
    }
  };

  if (loading) {
    return (
      <Layout>
        <div className="loading-state">Loading wallet...</div>
      </Layout>
    );
  }

  return (
    <Layout>
      <div className="dashboard">
        <h1>Welcome, {currentUser?.email}!</h1>

        {error && <div className="alert alert-error">{error}</div>}

        {wallet && (
          <>
            <WalletCard
              totalBalance={wallet.totalBalance}
              availableBalance={wallet.availableBalance}
              reservedBalance={wallet.reservedBalance}
              status={wallet.status}
              todaySpending={wallet.todaySpending}
            />

            <div className="load-money-card">
              <h2>💳 Add Money</h2>
              <div className="load-form">
                <div className="form-group">
                  <label htmlFor="loadAmount">Amount (₹)</label>
                  <input
                    id="loadAmount"
                    type="number"
                    step="0.01"
                    placeholder="0.00"
                    value={loadAmount}
                    onChange={(e) => setLoadAmount(e.target.value)}
                    disabled={loadingMoney}
                  />
                  {loadError && <span className="error">{loadError}</span>}
                </div>
                <button
                  className="btn btn-primary"
                  onClick={handleLoadMoneyClick}
                  disabled={loadingMoney || !loadAmount}
                >
                  {loadingMoney ? "Processing..." : "Add Money"}
                </button>
              </div>
              {loadSuccess && (
                <div className="alert alert-success">{loadSuccess}</div>
              )}
            </div>

            <MiniStatement />
          </>
        )}

        {showPinDialog && (
          <PinConfirmDialog
            onConfirm={handleLoadMoneyConfirm}
            onCancel={() => setShowPinDialog(false)}
            loading={loadingMoney}
          />
        )}
      </div>
    </Layout>
  );
}
